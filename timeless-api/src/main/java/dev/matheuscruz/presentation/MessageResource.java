package dev.matheuscruz.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.ai.ImageAiService;
import dev.matheuscruz.infra.ai.TextAiService;
import dev.matheuscruz.infra.ai.data.AiOperations;
import dev.matheuscruz.infra.ai.data.ContextMessage;
import dev.matheuscruz.infra.ai.data.RecordInfo;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/messages")
public class MessageResource {

    private final UserRepository userRepository;
    private final TextAiService aiService;
    private final ImageAiService imageAiService;
    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public MessageResource(TextAiService aiService, ImageAiService imageAiService, RecordRepository recordRepository,
            ObjectMapper mapper, UserRepository userRepository) {
        this.aiService = aiService;
        this.imageAiService = imageAiService;
        this.recordRepository = recordRepository;
        this.objectMapper = mapper;
        this.userRepository = userRepository;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response message(@Valid MessageRequest req) {
        User user = userRepository.findByPhoneNumber(req.from()).orElseThrow(NotFoundException::new);
        return handleMessage(user, req.message());
    }

    @POST
    @Path("/image")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response image(@Valid ImageRequest req) {

        User user = userRepository.findByPhoneNumber(req.from()).orElseThrow(NotFoundException::new);
        RecordInfo imageResponse = imageAiService.handleTransactionImage(
                Image.builder().base64Data(req.base64()).mimeType(req.mimeType()).build(), req.text());

        if (imageResponse.withError()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(imageResponse).build();
        }

        QuarkusTransaction.requiringNew().run(() -> {

            Record record = new Record.Builder().userId(user.getId()).amount(imageResponse.amount())
                    .description(imageResponse.description()).transaction(imageResponse.type())
                    .category(imageResponse.category()).build();

            this.recordRepository.persist(record);
        });

        return Response.status(Response.Status.CREATED).entity(imageResponse).build();
    }

    private Response handleMessage(User user, String message) {
        String response = aiService.handleMessage(message);
        return processAiResponse(user, response);
    }

    private Response processAiResponse(User user, String response) {

        ContextMessage contextMessage;
        try {
            contextMessage = objectMapper.readValue(response, ContextMessage.class);
        } catch (JsonProcessingException e) {
            Log.error("Failed to parse AI response", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (AiOperations.ADD_TRANSACTION.equals(contextMessage.operation())) {
            return handleTransaction(contextMessage, user);
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(contextMessage).build();
    }

    private Response handleTransaction(ContextMessage contextMessage, User user) {
        RecordInfo transaction;
        try {
            transaction = objectMapper.readValue(contextMessage.content(), RecordInfo.class);
        } catch (JsonProcessingException e) {
            Log.error("Failed to parse transaction content", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (transaction.withError()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        QuarkusTransaction.requiringNew().run(() -> {

            Record record = new Record.Builder().userId(user.getId()).amount(transaction.amount())
                    .description(transaction.description()).category(transaction.category())
                    .transaction(transaction.type()).build();

            recordRepository.persist(record);
        });

        return Response.ok(contextMessage).build();
    }

    public record MessageRequest(@NotBlank String from, @NotBlank String message) {
    }

    public record ImageRequest(@NotBlank String from, @NotBlank String base64, String text, @NotBlank String mimeType) {
    }
}
