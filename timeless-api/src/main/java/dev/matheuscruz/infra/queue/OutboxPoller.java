package dev.matheuscruz.infra.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.matheuscruz.domain.Outbox;
import dev.matheuscruz.domain.OutboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class OutboxPoller {

    private static final Logger LOGGER = Logger.getLogger(OutboxPoller.class);

    final OutboxRepository outboxRepository;
    final SqsClient sqs;
    final String processedMessagesUrl;
    final ObjectMapper objectMapper;

    public OutboxPoller(OutboxRepository outboxRepository, SqsClient sqs,
            @ConfigProperty(name = "whatsapp.recognized-message.queue-url") String processedMessagesUrl,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.sqs = sqs;
        this.processedMessagesUrl = processedMessagesUrl;
        this.objectMapper = objectMapper;
    }

    @Scheduled(every = "10s")
    @Transactional
    public void poll() {
        List<Outbox> pendingMessages = outboxRepository.findPending();
        if (pendingMessages.isEmpty()) {
            return;
        }

        LOGGER.infof("Processing %d pending outbox messages", pendingMessages.size());

        for (Outbox outbox : pendingMessages) {
            try {
                sendMessage(outbox);
                outbox.markAsSent();
                outboxRepository.persist(outbox);
            } catch (Exception e) {
                LOGGER.error("Failed to send outbox message: " + outbox.getId(), e);
                outbox.markAsFailed();
                outboxRepository.persist(outbox);
            }
        }
    }

    private void sendMessage(Outbox outbox) {
        sqs.sendMessage(req -> req.messageBody(outbox.getPayload()).messageGroupId("ProcessedMessages")
                .messageDeduplicationId(outbox.getId().toString()).queueUrl(processedMessagesUrl));
    }
}
