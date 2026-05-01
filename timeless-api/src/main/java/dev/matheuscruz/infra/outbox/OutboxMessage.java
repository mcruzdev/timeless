package dev.matheuscruz.infra.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "queue_url", nullable = false)
    private String queueUrl;

    @Column(name = "message_group_id", nullable = false)
    private String messageGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected OutboxMessage() {
    }

    public OutboxMessage(String payload, String queueUrl, String messageGroupId) {
        this.payload = payload;
        this.queueUrl = queueUrl;
        this.messageGroupId = messageGroupId;
        this.status = OutboxStatus.PENDING;
        this.createdAt = Instant.now();
        this.retryCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
        this.processedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
