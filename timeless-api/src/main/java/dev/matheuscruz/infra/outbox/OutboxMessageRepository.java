package dev.matheuscruz.infra.outbox;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxMessageRepository implements PanacheRepositoryBase<OutboxMessage, UUID> {

    private static final int MAX_RETRIES = 10;
    private static final int BATCH_SIZE = 20;

    public List<OutboxMessage> findPendingMessages() {
        return find("status = ?1 and retryCount < ?2 order by createdAt", OutboxStatus.PENDING, MAX_RETRIES)
                .range(0, BATCH_SIZE - 1).list();
    }
}
