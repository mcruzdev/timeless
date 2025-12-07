package dev.matheuscruz.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class RecordRepository implements PanacheRepository<Record> {

    public List<AmountAndTypeOnly> getRecordsWithAmountAndTypeOnlyByUser(String userId) {
        return find("select r.amount, r.transaction from Record as r where userId = :userId",
                Parameters.with("userId", userId)).project(AmountAndTypeOnly.class).list();
    }

    public List<AmountAndTypeOnly> getRecordsWithAmountAndTypeOnly() {
        return find("select r.amount, r.transaction from Record as r").project(AmountAndTypeOnly.class).list();
    }
}
