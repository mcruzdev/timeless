package dev.matheuscruz.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.matheuscruz.domain.OutcomeType;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.ai.TimelessAiService;
import dev.matheuscruz.infra.ai.TimelessImageAiService;
import dev.matheuscruz.infra.ai.data.AiResponse;
import dev.matheuscruz.infra.ai.data.AiTransactionResponse;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Parameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/messages")
public class MessageResource {

    private final UserRepository userRepository;
    private final TimelessAiService aiService;
    private final TimelessImageAiService imageAiService;
    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public MessageResource(TimelessAiService aiService, TimelessImageAiService imageAiService,
            RecordRepository recordRepository, ObjectMapper mapper, UserRepository userRepository) {
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
        User user = userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", req.from()))
                .firstResultOptional().orElseThrow(NotFoundException::new);
        return handleMessage(user, req.message());
    }

    @POST
    @Path("/image")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response image(@Valid ImageRequest req) {
        User user = userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", req.from()))
                .firstResultOptional().orElseThrow(NotFoundException::new);
        String imageResponse = imageAiService.handleTransactionImage(
                Image.builder().base64Data(req.base64()).mimeType(req.mimeType()).build(), req.text());
        return processAiResponse(user, imageResponse);
    }

    private Response handleMessage(User user, String message) {
        String response = aiService.handleMessage(message);
        return processAiResponse(user, response);
    }

    private Response processAiResponse(User user, String response) {

        AiResponse aiResponse;
        try {
            aiResponse = objectMapper.readValue(response, AiResponse.class);
        } catch (JsonProcessingException e) {
            Log.error("Failed to parse AI response", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if ("TRANSACTION".equals(aiResponse.operation())) {
            return handleTransaction(aiResponse, user);
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(aiResponse).build();
    }

    private Response handleTransaction(AiResponse aiResponse, User user) {
        AiTransactionResponse transaction;
        try {
            transaction = objectMapper.readValue(aiResponse.content(), AiTransactionResponse.class);
        } catch (JsonProcessingException e) {
            Log.error("Failed to parse transaction content", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (transaction.error()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        QuarkusTransaction.requiringNew().run(() -> {
            Record record = generateProperRecord(transaction, user.getId());
            recordRepository.persist(record);
        });

        return Response.ok(aiResponse).build();
    }

    private Record generateProperRecord(AiTransactionResponse transaction, String userId) {
        return transaction.type().equals(RecordType.OUT)
                ? Record.createOutcome(userId, transaction.amount(), transaction.description(), OutcomeType.NONE)
                : Record.createIncome(userId, transaction.amount(), transaction.description());
    }

    public record MessageRequest(@NotBlank String from, @NotBlank String message) {
    }

    public record ImageRequest(@NotBlank String from, @NotBlank String base64, String text, @NotBlank String mimeType) {
    }
}
