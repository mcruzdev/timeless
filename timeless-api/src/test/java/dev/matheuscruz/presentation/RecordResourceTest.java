package dev.matheuscruz.presentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.Transactions;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.presentation.data.UpdateRecordRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecordResourceTest {

    @Inject
    RecordRepository recordRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager em;

    public static LocalDate fixedDate = LocalDate.of(2026, 1, 1);

    @BeforeEach
    @Transactional
    void setUp() {
        recordRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user")
    @OidcSecurity(claims = { @Claim(key = "upn", value = "testUser") })
    void shouldUpdateRecord() {
        // Given
        Record record = new Record.Builder().userId("testUser").amount(new BigDecimal("100.00"))
                .description("Original Description").transaction(Transactions.OUT).category(Categories.FIXED_COSTS)
                .transactionDate(fixedDate).build();

        saveRecord(record);

        UpdateRecordRequest request = new UpdateRecordRequest(new BigDecimal("150.00"), "Updated Description",
                Transactions.IN, Categories.FINANCIAL_FREEDOM, fixedDate);

        // When
        given().contentType("application/json").body(request).when().put("/api/records/" + record.getId()).then()
                .statusCode(204);

        // Then
        em.clear();
        Record updatedRecord = recordRepository.findById(record.getId());
        assert updatedRecord != null;
        assert updatedRecord.getAmount().compareTo(new BigDecimal("150.00")) == 0;
        assert updatedRecord.getDescription().equals("Updated Description");
        assert updatedRecord.getTransaction() == Transactions.IN;
        assert updatedRecord.getCategory() == Categories.FINANCIAL_FREEDOM;
        assert updatedRecord.getTransactionDate().equals(fixedDate);
    }

    @Test
    @TestSecurity(user = "otherUser", roles = "user")
    @OidcSecurity(claims = { @Claim(key = "upn", value = "otherUser") })
    void shouldNotUpdateRecordOfAnotherUser() {
        // Given
        Record record = new Record.Builder().userId("testUser").amount(new BigDecimal("100.00"))
                .description("Original Description").transaction(Transactions.OUT).category(Categories.FIXED_COSTS)
                .transactionDate(fixedDate).build();

        saveRecord(record);

        UpdateRecordRequest request = new UpdateRecordRequest(new BigDecimal("150.00"), "Updated Description",
                Transactions.IN, Categories.FINANCIAL_FREEDOM, fixedDate);

        // When
        given().contentType("application/json").body(request).when().put("/api/records/" + record.getId()).then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user")
    @OidcSecurity(claims = { @Claim(key = "upn", value = "testUser") })
    void shouldReturnNotFoundWhenRecordDoesNotExist() {
        UpdateRecordRequest request = new UpdateRecordRequest(new BigDecimal("150.00"), "Updated Description",
                Transactions.IN, Categories.FINANCIAL_FREEDOM, LocalDate.now());

        given().contentType("application/json").body(request).when().put("/api/records/999").then().statusCode(404);
    }

    @Transactional
    void saveRecord(Record record) {
        recordRepository.persist(record);
    }
}
