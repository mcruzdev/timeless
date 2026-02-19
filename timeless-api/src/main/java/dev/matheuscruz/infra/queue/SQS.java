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
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SQS {

    final SqsClient sqs;
    final ObjectMapper objectMapper;
    final TextAiService aiService;
    final RecordRepository recordRepository;
    final UserRepository userRepository;
    final Logger logger = Logger.getLogger(SQS.class);

    private static final ObjectReader INCOMING_MESSAGE_READER = new ObjectMapper().readerFor(IncomingMessage.class);

    private static final ObjectReader AI_RESPONSE_READER = new ObjectMapper().readerFor(RecognizedOperation.class);

    public SQS(SqsClient sqs, ObjectMapper objectMapper, TextAiService aiService, RecordRepository recordRepository,
            UserRepository userRepository) {

        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @Incoming("whatsapp-incoming")
    @Outgoing("whatsapp-recognized")
    public Multi<Message<String>> receiveMessages(Message<String> message) {
        String body = message.getPayload();
        IncomingMessage incomingMessage = parseIncomingMessage(body);

        if (!MessageKind.TEXT.equals(incomingMessage.kind())) {
            return Multi.createFrom().item(message);
        }

        Optional<User> user = this.userRepository.findByPhoneNumber(incomingMessage.sender());

        if (user.isEmpty()) {
            logger.error("User not found.");
            return Multi.createFrom().empty();
        }

        return Multi.createFrom().iterable(handleUserMessage(user.get(), incomingMessage)).map(processedMessage -> {
            try {
                String processedBody = objectMapper.writeValueAsString(processedMessage);
                return Message.of(processedBody).withAck(() -> message.ack())
                        .withNack(throwable -> message.nack(throwable));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize message", e);
                throw new RuntimeException(e);
            }
        });
    }

    private List<Object> handleUserMessage(User user, IncomingMessage message) {
        List<Object> results = new ArrayList<>();
        try {
            AllRecognizedOperations allRecognizedOperations = aiService.handleMessage(message.messageBody(),
                    user.getId());

            for (RecognizedOperation recognizedOperation : allRecognizedOperations.all()) {
                switch (recognizedOperation.operation()) {
                    case AiOperations.ADD_TRANSACTION ->
                            results.add(processAddTransactionMessage(user, message, recognizedOperation));
                    case AiOperations.GET_BALANCE -> {
                        logger.info("Processing GET_BALANCE operation" + recognizedOperation.recognizedTransaction());
                        results.add(processSimpleMessage(user, message, recognizedOperation));
                    }
                    default -> logger.warnf("Unknown operation type: %s", recognizedOperation.operation());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process message: " + message.messageId(), e);
        }
        return results;
    }

    private TransactionMessageProcessed processAddTransactionMessage(User user, IncomingMessage message,
            RecognizedOperation recognizedOperation) {
        RecognizedTransaction recognizedTransaction = recognizedOperation.recognizedTransaction();

        Record record = new Record.Builder().userId(user.getId()).amount(recognizedTransaction.amount())
                .description(recognizedTransaction.description()).transaction(recognizedTransaction.type())
                .category(recognizedTransaction.category()).build();

        QuarkusTransaction.requiringNew().run(() -> recordRepository.persist(record));

        logger.infof("Message %s processed as ADD_TRANSACTION", message.messageId());

        return new TransactionMessageProcessed(AiOperations.ADD_TRANSACTION.commandName(), message.messageId(),
                MessageStatus.PROCESSED, user.getPhoneNumber(), recognizedTransaction.withError(),
                recognizedTransaction);
    }

    private SimpleMessageProcessed processSimpleMessage(User user, IncomingMessage message,
            RecognizedOperation recognizedOperation) {
        logger.infof("Processing simple message for user %s", recognizedOperation.recognizedTransaction());
        SimpleMessage response = new SimpleMessage(recognizedOperation.recognizedTransaction().description());

        logger.infof("Message %s processed as GET_BALANCE", message.messageId());

        return new SimpleMessageProcessed(AiOperations.GET_BALANCE.commandName(), message.messageId(),
                MessageStatus.PROCESSED, user.getPhoneNumber(), response);
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
