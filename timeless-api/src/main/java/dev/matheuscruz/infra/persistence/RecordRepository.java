package dev.matheuscruz.infra.persistence;

import dev.matheuscruz.domain.Record;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecordRepository implements PanacheRepository<Record> {
}
