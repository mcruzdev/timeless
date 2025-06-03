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
import dev.matheuscruz.infra.ai.data.ContextMessage;
import dev.matheuscruz.infra.ai.data.RecordInfo;
import dev.matheuscruz.infra.ai.data.SimpleMessage;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SQS {

    final String incomingMessagesUrl;
    final String processedMessagesUrl;
    final SqsClient sqs;
    final ObjectMapper objectMapper;
    final TextAiService aiService;
    final RecordRepository recordRepository;
    final UserRepository userRepository;
    final Logger logger = Logger.getLogger(SQS.class);

    private static final ObjectReader INCOMING_MESSAGE_READER = new ObjectMapper().readerFor(IncomingMessage.class);

    private static final ObjectReader AI_RESPONSE_READER = new ObjectMapper().readerFor(ContextMessage.class);

    public SQS(SqsClient sqs, @ConfigProperty(name = "whatsapp.incoming-messages.queue-url") String incomingMessagesUrl,
            @ConfigProperty(name = "whatsapp.messages-processed.queue-url") String messagesProcessedUrl,
            ObjectMapper objectMapper, TextAiService aiService, RecordRepository recordRepository,
            UserRepository userRepository) {

        this.sqs = sqs;
        this.incomingMessagesUrl = incomingMessagesUrl;
        this.processedMessagesUrl = messagesProcessedUrl;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(every = "5s")
    public void receiveMessages() {
        sqs.receiveMessage(req -> req.maxNumberOfMessages(10).queueUrl(incomingMessagesUrl)).messages()
                .forEach(message -> processMessage(message.body(), message.receiptHandle()));
    }

    private void processMessage(String body, String receiptHandle) {
        IncomingMessage incomingMessage = parseIncomingMessage(body);
        if (!MessageKind.TEXT.equals(incomingMessage.kind()))
            return;

        Optional<User> user = this.userRepository.findByPhoneNumber(incomingMessage.sender());

        if (user.isEmpty()) {
            logger.error("User not found. Deleting message from queue.");
            deleteMessageUsing(receiptHandle);
            return;
        }

        handleUserMessage(user.get(), incomingMessage, receiptHandle);
    }

    private void handleUserMessage(User user, IncomingMessage message, String receiptHandle) {
        try {
            ContextMessage contextMessage = parseAiResponse(aiService.handleMessage(message.messageBody()));

            switch (contextMessage.operation()) {
                case AiOperations.ADD_TRANSACTION ->
                        processTransactionMessage(user, message, receiptHandle, contextMessage);
                case AiOperations.GET_BALANCE -> processSimpleMessage(user, message, receiptHandle, contextMessage);
                default -> logger.warnf("Unknown operation type: %s", contextMessage.operation());
            }

        } catch (Exception e) {
            logger.error("Failed to process message: " + message.messageId(), e);
        }
    }

    private void processTransactionMessage(User user, IncomingMessage message, String receiptHandle,
            ContextMessage contextMessage) throws IOException {
        RecordInfo transaction = objectMapper.readValue(contextMessage.content(), RecordInfo.class);

        sendProcessedMessage(
                new TransactionMessageProcessed(AiOperations.ADD_TRANSACTION.commandName(), message.messageId(),
                        MessageStatus.PROCESSED, user.getPhoneNumber(), transaction.withError(), transaction));

        Record record = new Record.Builder().userId(user.getId()).amount(transaction.amount())
                .description(transaction.description()).transaction(transaction.type()).category(transaction.category())
                .build();

        QuarkusTransaction.requiringNew().run(() -> recordRepository.persist(record));

        deleteMessageUsing(receiptHandle);

        logger.infof("Message %s processed as ADD_TRANSACTION", message.messageId());
    }

    private void processSimpleMessage(User user, IncomingMessage message, String receiptHandle,
            ContextMessage contextMessage) throws IOException {
        SimpleMessage response = new SimpleMessage(contextMessage.content());
        sendProcessedMessage(new SimpleMessageProcessed(AiOperations.GET_BALANCE.commandName(), message.messageId(),
                MessageStatus.PROCESSED, user.getPhoneNumber(), response));
        deleteMessageUsing(receiptHandle);
        logger.infof("Message %s processed as GET_BALANCE", message.messageId());
    }

    private void sendProcessedMessage(Object processedMessage) throws JsonProcessingException {
        String messageBody = objectMapper.writeValueAsString(processedMessage);
        sqs.sendMessage(req -> req.messageBody(messageBody).messageGroupId("ProcessedMessages")
                .messageDeduplicationId(UUID.randomUUID().toString()).queueUrl(processedMessagesUrl));
    }

    private ContextMessage parseAiResponse(String response) {
        try {
            return AI_RESPONSE_READER.readValue(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    private IncomingMessage parseIncomingMessage(String messageBody) {
        try {
            return INCOMING_MESSAGE_READER.readValue(messageBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse incoming message", e);
        }
    }

    private void deleteMessageUsing(String receiptHandle) {
        sqs.deleteMessage(req -> req.queueUrl(incomingMessagesUrl).receiptHandle(receiptHandle));
    }

    public record TransactionMessageProcessed(String kind, String messageId, MessageStatus status, String user,
            Boolean withError, RecordInfo content) {
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
