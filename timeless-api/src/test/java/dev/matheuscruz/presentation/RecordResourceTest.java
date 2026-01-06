package dev.matheuscruz.presentation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.Transactions;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.presentation.data.CreateRecordRequest;
import dev.matheuscruz.presentation.data.PageRecord;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecordResourceTest {

    @Inject
    RecordRepository recordRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        QuarkusTransaction.requiringNew().run(() -> {
            recordRepository.deleteAll();
            userRepository.deleteAll();
        });
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "upn", value = "test-user-id") })
    @DisplayName("Should return empty list when user has no records")
    void should_returnEmptyList_when_userHasNoRecords() {
        var response = given().when().get("/api/records").then().statusCode(200).extract().as(PageRecord.class);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalRecords()).isZero();
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "upn", value = "test-user-id") })
    @DisplayName("Should return records when user has records")
    void should_returnRecords_when_userHasRecords() throws Exception {
        String userId = "test-user-id";
        User user = createUser(userId, "+5511999999999");

        QuarkusTransaction.requiringNew().run(() -> {
            userRepository.persist(user);
            recordRepository.persist(new Record.Builder().userId(userId).amount(new BigDecimal("50.00"))
                    .description("Test record").transaction(Transactions.OUT).category(Categories.GENERAL).build());
        });

        var response = given().when().get("/api/records").then().statusCode(200).extract().as(PageRecord.class);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalRecords()).isEqualTo(1L);
        assertThat(response.totalExpenses()).isEqualByComparingTo("50.00");
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "upn", value = "test-user-id") })
    @DisplayName("Should create record successfully when valid request and matching user")
    void should_createRecord_when_validRequestAndMatchingUser() throws Exception {
        String userId = "test-user-id";
        String phoneNumber = "+5511999999999";
        User user = createUser(userId, phoneNumber);

        QuarkusTransaction.requiringNew().run(() -> userRepository.persist(user));

        CreateRecordRequest req = new CreateRecordRequest(new BigDecimal("100.00"), "New Record", Transactions.OUT,
                phoneNumber, Categories.GENERAL);

        given().contentType(ContentType.JSON).body(req).when().post("/api/records").then().statusCode(201);

        List<Record> records = recordRepository.list("userId", userId);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getDescription()).isEqualTo("New Record");
        assertThat(records.get(0).getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "upn", value = "test-user-id") })
    @DisplayName("Should return forbidden when creating record for another user")
    void should_returnForbidden_when_creatingRecordForAnotherUser() throws Exception {
        String otherUserId = "other-user-id";
        String otherPhoneNumber = "+5511888888888";
        User otherUser = createUser(otherUserId, otherPhoneNumber);

        QuarkusTransaction.requiringNew().run(() -> userRepository.persist(otherUser));

        CreateRecordRequest req = new CreateRecordRequest(new BigDecimal("100.00"), "Other Record", Transactions.OUT,
                otherPhoneNumber, Categories.GENERAL);

        given().contentType(ContentType.JSON).body(req).when().post("/api/records").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "upn", value = "test-user-id") })
    @DisplayName("Should delete record successfully when owned by user")
    void should_deleteRecord_when_ownedByUser() throws Exception {
        String userId = "test-user-id";
        User user = createUser(userId, "+5511999999999");

        Long recordId = QuarkusTransaction.requiringNew().call(() -> {
            userRepository.persist(user);
            Record record = new Record.Builder().userId(userId).amount(new BigDecimal("50.00"))
                    .description("To be deleted").transaction(Transactions.OUT).category(Categories.GENERAL).build();
            recordRepository.persist(record);
            return record.getId();
        });

        given().when().delete("/api/records/" + recordId).then().statusCode(204);

        assertThat(recordRepository.findById(recordId)).isNull();
    }

    private User createUser(String id, String phoneNumber) throws Exception {
        User user = User.create("test-" + id + "@test.com", "password", "First", "Last", phoneNumber);
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
