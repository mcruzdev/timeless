package dev.matheuscruz.infra.persistence;

import dev.matheuscruz.domain.Record;
import dev.matheuscruz.infra.persistence.projection.AmountTypeProjection;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class RecordRepository implements PanacheRepository<Record> {

    public List<AmountTypeProjection> getRecordsWithAmountAndTypeOnly() {
        return find("select r.amount, r.recordType from Record as r").project(AmountTypeProjection.class).list();
    }
}
