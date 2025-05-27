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
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Parameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/messages")
public class MessageResource {

    UserRepository userRepository;
    TimelessAiService aiService;
    TimelessImageAiService imageAiService;
    RecordRepository recordRepository;
    ObjectMapper mapper;
    String assetsBucket;

    public MessageResource(TimelessAiService aiService, TimelessImageAiService imageAiService,
            RecordRepository recordRepository, ObjectMapper mapper,
            @ConfigProperty(name = "assets.bucket") String assetsBucket, UserRepository userRepository) {
        this.aiService = aiService;
        this.imageAiService = imageAiService;
        this.recordRepository = recordRepository;
        this.mapper = mapper;
        this.assetsBucket = assetsBucket;
        this.userRepository = userRepository;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response message(@Valid MessageRequest req) {
        User user = userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", req.from()))
                .firstResultOptional().orElseThrow(NotFoundException::new);

        AiResponse aiResponse = aiService.identifyTransaction(req.message());

        if (aiResponse.error()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {

            QuarkusTransaction.requiringNew().run(() -> {
                Record record = generateProperRecord(aiResponse, user.getId());
                recordRepository.persist(record);
            });

            return Response.status(Response.Status.OK).entity(aiResponse).build();
        }

    }

    @POST
    @Path("/image")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response image(@Valid ImageRequest req) {

        User user = userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", req.from()))
                .firstResultOptional().orElseThrow(NotFoundException::new);

        String response = this.imageAiService.identifyTransaction(
                Image.builder().base64Data(req.base64()).mimeType(req.mimeType()).build(), req.text());

        try {
            AiResponse aiResponse = this.mapper.readValue(response, AiResponse.class);
            if (aiResponse.error()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            QuarkusTransaction.requiringNew().run(() -> {
                Record record = generateProperRecord(aiResponse, user.getId());
                recordRepository.persist(record);
            });

            return Response.status(Response.Status.OK).entity(aiResponse).build();

        } catch (JsonProcessingException e) {
            Log.info(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Record generateProperRecord(AiResponse aiResponse, String userId) {
        if (aiResponse.type().equals(RecordType.OUT)) {
            return Record.createOutcome(userId, aiResponse.amount(), aiResponse.description(), OutcomeType.NONE);
        } else {
            return Record.createIncome(userId, aiResponse.amount(), aiResponse.description());
        }
    }

    public record AiResponse(BigDecimal amount, String description, Boolean error, RecordType type) {
    }

    public record MessageRequest(@NotBlank String from, @NotBlank String message) {
    }

    public record ImageRequest(@NotBlank String from, @NotBlank String base64, String text, @NotBlank String mimeType) {
    }
}
