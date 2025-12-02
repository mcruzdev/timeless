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

    private static final ObjectReader AI_RESPONSE_READER = new ObjectMapper().readerFor(RecognizedOperation.class);

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
            AllRecognizedOperations allRecognizedOperations = aiService.handleMessage(message.messageBody());

            for (RecognizedOperation recognizedOperation : allRecognizedOperations.all()) {
                switch (recognizedOperation.operation()) {
                    case AiOperations.ADD_TRANSACTION ->
                            processAddTransactionMessage(user, message, receiptHandle, recognizedOperation);
                    case AiOperations.GET_BALANCE -> {
                        logger.info("Processing GET_BALANCE operation" + recognizedOperation.recognizedTransaction());
                        processSimpleMessage(user, message, receiptHandle, recognizedOperation);
                    }
                    default -> logger.warnf("Unknown operation type: %s", recognizedOperation.operation());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to process message: " + message.messageId(), e);
        }
    }

    private void processAddTransactionMessage(User user, IncomingMessage message, String receiptHandle,
            RecognizedOperation recognizedOperation) throws IOException {
        RecognizedTransaction recognizedTransaction = recognizedOperation.recognizedTransaction();
        sendProcessedMessage(new TransactionMessageProcessed(AiOperations.ADD_TRANSACTION.commandName(),
                message.messageId(), MessageStatus.PROCESSED, user.getPhoneNumber(), recognizedTransaction.withError(),
                recognizedTransaction));

        Record record = new Record.Builder().userId(user.getId()).amount(recognizedTransaction.amount())
                .description(recognizedTransaction.description()).transaction(recognizedTransaction.type())
                .category(recognizedTransaction.category()).build();

        QuarkusTransaction.requiringNew().run(() -> recordRepository.persist(record));

        deleteMessageUsing(receiptHandle);

        logger.infof("Message %s processed as ADD_TRANSACTION", message.messageId());
    }

    private void processSimpleMessage(User user, IncomingMessage message, String receiptHandle,
            RecognizedOperation recognizedOperation) throws IOException {
        logger.infof("Processing simple message for user %s", recognizedOperation.recognizedTransaction());
        SimpleMessage response = new SimpleMessage(recognizedOperation.recognizedTransaction().description());
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
