package dev.matheuscruz.infra.outbox;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ApplicationScoped
public class OutboxMessageRelay {

    private static final int MAX_RETRIES = 10;

    private final OutboxMessageRepository outboxMessageRepository;
    private final SqsClient sqsClient;
    private final Logger logger = Logger.getLogger(OutboxMessageRelay.class);

    public OutboxMessageRelay(OutboxMessageRepository outboxMessageRepository, SqsClient sqsClient) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.sqsClient = sqsClient;
    }

    @Scheduled(every = "5s")
    public void processOutbox() {
        List<OutboxMessage> pending = outboxMessageRepository.findPendingMessages();

        for (OutboxMessage message : pending) {
            if (message.getRetryCount() >= MAX_RETRIES) {
                failMessage(message);
                continue;
            }

            try {
                sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(message.getQueueUrl())
                        .messageBody(message.getPayload()).messageGroupId(message.getMessageGroupId()).build());

                updateMessageStatus(message, true);
            } catch (Exception e) {
                logger.errorf(e, "Failed to send outbox message %s to queue %s", message.getId(),
                        message.getQueueUrl());
                updateMessageStatus(message, false);
            }
        }
    }

    private void updateMessageStatus(OutboxMessage message, boolean success) {
        QuarkusTransaction.requiringNew().run(() -> {
            OutboxMessage managed = outboxMessageRepository.findById(message.getId());
            if (managed != null) {
                if (success) {
                    managed.markAsSent();
                } else {
                    managed.incrementRetryCount();
                }
            }
        });
    }

    private void failMessage(OutboxMessage message) {
        QuarkusTransaction.requiringNew().run(() -> {
            OutboxMessage managed = outboxMessageRepository.findById(message.getId());
            if (managed != null) {
                managed.markAsFailed();
                logger.errorf("Outbox message %s has exceeded max retries, marking as FAILED", message.getId());
            }
        });
    }
}
