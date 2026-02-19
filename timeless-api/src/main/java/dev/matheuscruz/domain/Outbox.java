package dev.matheuscruz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox")
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status;

    private Instant createdAt;

    private Instant processedAt;

    protected Outbox() {
    }

    public Outbox(String payload) {
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.processedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }
}
