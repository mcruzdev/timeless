package dev.matheuscruz.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class RecordRepository implements PanacheRepository<Record> {

    @Inject
    EntityManager em;

    public List<AmountAndTypeOnly> getRecordsWithAmountAndTypeOnlyByUser(String userId) {
        Log.info("Getting balance for user ID: " + userId);
        return find("select r.amount, r.transaction from Record as r where userId = :userId",
                Parameters.with("userId", userId)).project(AmountAndTypeOnly.class).list();
    }

    public RecordSummary getRecordSummary(String userId, int page, int limit) {

        Tuple aggregates = em
                .createQuery("select count(r), " + "sum(case when r.transaction = :out then r.amount else 0 end), "
                        + "sum(case when r.transaction = :in then r.amount else 0 end) "
                        + "from Record r where r.userId = :userId", Tuple.class)
                .setParameter("userId", userId).setParameter("out", Transactions.OUT)
                .setParameter("in", Transactions.IN).getSingleResult();

        long totalRecords = aggregates.get(0, Long.class);
        BigDecimal totalExpenses = aggregates.get(1, BigDecimal.class);
        BigDecimal totalIncome = aggregates.get(2, BigDecimal.class);

        BigDecimal[] verificationTotal = verificationTotal(totalExpenses, totalIncome);

        List<Record> records = find("userId = :userId", Parameters.with("userId", userId)).page(Page.of(page, limit))
                .list();

        return new RecordSummary(records, totalRecords, verificationTotal[0], verificationTotal[1]);
    }

    private BigDecimal[] verificationTotal(BigDecimal expenses, BigDecimal income) {
        return new BigDecimal[] { expenses == null ? BigDecimal.ZERO : expenses,
                income == null ? BigDecimal.ZERO : income };
    }
}
