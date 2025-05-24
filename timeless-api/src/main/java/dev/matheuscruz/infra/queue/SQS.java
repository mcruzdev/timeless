package dev.matheuscruz.infra.queue;

import java.math.BigDecimal;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.ai.TimelessAiService;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.presentation.MessageResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Parameters;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SQS {

    final String incomingMessagesUrl;
    final String processedMessagesUrl;
    final SqsClient sqs;
    final ObjectMapper objectMapper;
    final Logger logger = Logger.getLogger(SQS.class);
    final TimelessAiService aiService;
    final RecordRepository recordRepository;
    final UserRepository userRepository;

    static ObjectReader INCOMING_MESSAGE_READER = new ObjectMapper()
            .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true)
            .readerFor(IncomingMessage.class);

    public SQS(SqsClient sqs, @ConfigProperty(name = "whatsapp.incoming-messages.queue-url") String incomingMessagesUrl,
            @ConfigProperty(name = "whatsapp.messages-processed.queue-url") String messagesProcessedUrl,
            ObjectMapper objectMapper, TimelessAiService aiService, RecordRepository recordRepository,
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
    public void receive() {
        sqs.receiveMessage(m -> m.maxNumberOfMessages(10).queueUrl(this.incomingMessagesUrl)).messages()
                .forEach(message -> {
                    String handle = message.receiptHandle();
                    IncomingMessage incomingMessage = toIncomingMessage(message.body());

                    if (incomingMessage.kind().equals("text")) {
                        handleTextMessage(incomingMessage, handle);
                    }
                });
    }

    private void handleTextMessage(IncomingMessage incomingMessage, String receiptHandle) {
        try {
            Optional<User> user = this.userRepository
                    .find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", incomingMessage.sender()))
                    .firstResultOptional();

            if (user.isPresent()) {
                MessageResource.AiResponse aiResponse = this.aiService
                        .identifyTransaction(incomingMessage.messageBody());
                logger.info(aiResponse);
                QuarkusTransaction.requiringNew().run(() -> this.recordRepository.persist(Record
                        .create(user.get().getId(), aiResponse.amount(), aiResponse.description(), aiResponse.type())));
                String messageProcessedEvent = this.objectMapper.writeValueAsString(new MessageProcessed(
                        incomingMessage.messageId(), "PROCESSED", incomingMessage.chat(), aiResponse.error(),
                        new RecordResponse(aiResponse.description(), aiResponse.amount(), aiResponse.type())));
                sqs.sendMessage(builder -> builder.messageBody(messageProcessedEvent)
                        .queueUrl(this.processedMessagesUrl).messageGroupId("ProcessedMessages").build());
            } else {
                logger.error("User not found, deleting message from Queue");
            }

            // the message deletion can fail
            // take care about duplications
            sqs.deleteMessage(builder -> builder.queueUrl(this.incomingMessagesUrl).receiptHandle(receiptHandle));

            logger.infof("message with ID %s processed successfully", incomingMessage.messageId());
        } catch (JsonProcessingException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IncomingMessage toIncomingMessage(String messageBody) {
        try {
            return INCOMING_MESSAGE_READER.readValue(messageBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public record MessageProcessed(String messageId, String status, Chat chat, Boolean withError,
            RecordResponse record) {
    }

    public record RecordResponse(String description, BigDecimal amount, RecordType type) {
    }

    public record IncomingMessage(String sender, String kind, String messageId, Chat chat, String status,
            String messageBody) {
    }

    public record Chat(String server, String user, String _serialized) {
    }
}
