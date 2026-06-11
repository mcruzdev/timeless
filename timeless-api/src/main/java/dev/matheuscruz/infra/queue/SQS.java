package dev.matheuscruz.infra.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.ai.TextAiService;
import dev.matheuscruz.infra.ai.data.AiOperations;
import dev.matheuscruz.infra.ai.data.AllRecognizedOperations;
import dev.matheuscruz.infra.ai.data.RecognizedOperation;
import dev.matheuscruz.infra.ai.data.RecognizedTransaction;
import dev.matheuscruz.infra.ai.data.SimpleMessage;
import dev.matheuscruz.infra.outbox.OutboxMessage;
import dev.matheuscruz.infra.outbox.OutboxMessageRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SQS {

    final ObjectMapper objectMapper;
    final TextAiService aiService;
    final RecordRepository recordRepository;
    final UserRepository userRepository;
    final OutboxMessageRepository outboxMessageRepository;
    final String recognizedQueueUrl;
    final Logger logger = Logger.getLogger(SQS.class);

    private static final ObjectReader INCOMING_MESSAGE_READER = new ObjectMapper().readerFor(IncomingMessage.class);

    public SQS(ObjectMapper objectMapper, TextAiService aiService, RecordRepository recordRepository,
            UserRepository userRepository, OutboxMessageRepository outboxMessageRepository,
            @ConfigProperty(name = "whatsapp.recognized-message.queue-url") String recognizedQueueUrl) {

        this.objectMapper = objectMapper;
        this.aiService = aiService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.recognizedQueueUrl = recognizedQueueUrl;
    }

    @Incoming("whatsapp-incoming")
    public CompletionStage<Void> receiveMessages(Message<String> message) {
        String body = message.getPayload();
        IncomingMessage incomingMessage = parseIncomingMessage(body);

        if (!MessageKind.TEXT.equals(incomingMessage.kind())) {
            return message.ack();
        }

        Optional<User> user = this.userRepository.findByPhoneNumber(incomingMessage.sender());

        if (user.isEmpty()) {
            logger.error("User not found.");
            return message.nack(new RuntimeException("User not found for phone: " + incomingMessage.sender()));
        }

        try {
            handleUserMessage(user.get(), incomingMessage);
            return message.ack();
        } catch (Exception e) {
            logger.error("Failed to process message: " + incomingMessage.messageId(), e);
            return message.nack(e);
        }
    }

    private void handleUserMessage(User user, IncomingMessage message) {
        AllRecognizedOperations allRecognizedOperations = aiService.handleMessage(message.messageBody(), user.getId());

        for (RecognizedOperation recognizedOperation : allRecognizedOperations.all()) {
            switch (recognizedOperation.operation()) {
                case AiOperations.ADD_TRANSACTION -> processAddTransactionMessage(user, message, recognizedOperation);
                case AiOperations.GET_BALANCE -> {
                    logger.info("Processing GET_BALANCE operation" + recognizedOperation.recognizedTransaction());
                    processSimpleMessage(user, message, recognizedOperation);
                }
                default -> logger.warnf("Unknown operation type: %s", recognizedOperation.operation());
            }
        }
    }

    private void processAddTransactionMessage(User user, IncomingMessage message,
            RecognizedOperation recognizedOperation) {
        RecognizedTransaction recognizedTransaction = recognizedOperation.recognizedTransaction();

        Record record = new Record.Builder().userId(user.getId()).amount(recognizedTransaction.amount())
                .description(recognizedTransaction.description()).transaction(recognizedTransaction.type())
                .category(recognizedTransaction.category()).build();

        TransactionMessageProcessed processed = new TransactionMessageProcessed(
                AiOperations.ADD_TRANSACTION.commandName(), message.messageId(), MessageStatus.PROCESSED,
                user.getPhoneNumber(), recognizedTransaction.withError(), recognizedTransaction);

        OutboxMessage outboxMessage = new OutboxMessage(serialize(processed), recognizedQueueUrl,
                user.getPhoneNumber());

        QuarkusTransaction.requiringNew().run(() -> {
            recordRepository.persist(record);
            outboxMessageRepository.persist(outboxMessage);
        });

        logger.infof("Message %s processed as ADD_TRANSACTION", message.messageId());
    }

    private void processSimpleMessage(User user, IncomingMessage message, RecognizedOperation recognizedOperation) {
        logger.infof("Processing simple message for user %s", recognizedOperation.recognizedTransaction());
        SimpleMessage response = new SimpleMessage(recognizedOperation.recognizedTransaction().description());

        SimpleMessageProcessed processed = new SimpleMessageProcessed(AiOperations.GET_BALANCE.commandName(),
                message.messageId(), MessageStatus.PROCESSED, user.getPhoneNumber(), response);

        OutboxMessage outboxMessage = new OutboxMessage(serialize(processed), recognizedQueueUrl,
                user.getPhoneNumber());

        QuarkusTransaction.requiringNew().run(() -> outboxMessageRepository.persist(outboxMessage));

        logger.infof("Message %s processed as GET_BALANCE", message.messageId());
    }

    private String serialize(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    private IncomingMessage parseIncomingMessage(String messageBody) {
        try {
            return INCOMING_MESSAGE_READER.readValue(messageBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse incoming message", e);
        }
    }

    public record TransactionMessageProcessed(String kind, String messageId, MessageStatus status, String user,
            Boolean withError, RecognizedTransaction content) {
    }

    public record SimpleMessageProcessed(String kind, String messageId, MessageStatus status, String user,
            SimpleMessage content) {
    }

    public record IncomingMessage(String sender, MessageKind kind, String messageId, MessageStatus status,
            String messageBody) {
    }

    public enum MessageKind {
        TEXT, AUDIO, IMAGE
    }

    public enum MessageStatus {
        READ, PROCESSED
    }
}
