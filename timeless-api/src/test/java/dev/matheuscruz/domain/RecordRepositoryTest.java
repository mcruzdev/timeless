package dev.matheuscruz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecordRepositoryTest {

    @Inject
    RecordRepository recordRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        recordRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("Should return record summary correctly for a given user")
    void shouldReturnRecordSummaryCorrectly() {

        String userId = "user-" + Instancio.create(String.class);

        List<Record> recordsToPersist = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            recordsToPersist.add(
                    new Record.Builder().userId(userId).transaction(Transactions.OUT).amount(new BigDecimal("10.00"))
                            .description(Instancio.create(String.class)).category(Categories.GENERAL).build());
        }

        for (int i = 0; i < 2; i++) {
            recordsToPersist.add(
                    new Record.Builder().userId(userId).transaction(Transactions.IN).amount(new BigDecimal("50.00"))
                            .description(Instancio.create(String.class)).category(Categories.NONE).build());
        }

        for (int i = 0; i < 5; i++) {
            recordsToPersist.add(new Record.Builder().userId("other-" + userId).transaction(Transactions.OUT)
                    .amount(new BigDecimal("5.00")).description("Other " + i).category(Categories.FIXED_COSTS).build());
        }

        recordsToPersist.forEach(recordRepository::persist);

        RecordSummary summary = recordRepository.getRecordSummary(userId, 0, 10);

        assertThat(summary).isNotNull();
        assertThat(summary.totalRecords()).isEqualTo(5);
        assertThat(summary.totalExpenses()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(summary.totalIncome()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(summary.records()).hasSize(5);
    }

    @Test
    @Transactional
    @DisplayName("Should return zeroed summary when user has no records")
    void shouldReturnZeroedSummaryWhenNoRecords() {
        RecordSummary summary = recordRepository.getRecordSummary("empty-" + Instancio.create(String.class), 0, 10);

        assertThat(summary).isNotNull();
        assertThat(summary.totalRecords()).isZero();
        assertThat(summary.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.records()).isEmpty();
    }
}
