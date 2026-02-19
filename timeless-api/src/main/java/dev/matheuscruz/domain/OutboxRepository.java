package dev.matheuscruz.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<Outbox> {

    public List<Outbox> findPending() {
        return list("status", "PENDING");
    }
}
