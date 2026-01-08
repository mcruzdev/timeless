package dev.matheuscruz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inbound_messages")
public class InboundMessage {

    @Id
    private String messageId;

    private Instant processedAt;

    protected InboundMessage() {
    }

    public InboundMessage(String messageId) {
        this.messageId = messageId;
        this.processedAt = Instant.now();
    }

    public String getMessageId() {
        return messageId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
