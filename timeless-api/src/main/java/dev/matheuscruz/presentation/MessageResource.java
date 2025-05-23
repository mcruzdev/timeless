package dev.matheuscruz.presentation;

import java.math.BigDecimal;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.matheuscruz.domain.OutcomeType;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.ai.TimelessAiService;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Parameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/messages")
public class MessageResource {

    UserRepository userRepository;
    TimelessAiService aiService;
    RecordRepository recordRepository;
    ObjectMapper mapper;
    String assetsBucket;

    public MessageResource(TimelessAiService aiService, RecordRepository recordRepository, ObjectMapper mapper,
            @ConfigProperty(name = "assets.bucket") String assetsBucket, UserRepository userRepository) {
        this.aiService = aiService;
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

        String json = aiService.identifyTransaction(req.message());

        try {
            AiResponse aiResponse = mapper.readValue(json, AiResponse.class);
            if (aiResponse.error()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {

                QuarkusTransaction.requiringNew().run(() -> {
                    Record record = generateProperRecord(aiResponse, user);
                    recordRepository.persist(record);
                });

                return Response.status(Response.Status.OK).entity(aiResponse).build();
            }
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Record generateProperRecord(AiResponse aiResponse, User user) {
        if (aiResponse.type().equals(RecordType.OUT)) {
            return Record.createOutcome(user.getId(), aiResponse.amount(), aiResponse.description(), OutcomeType.NONE);
        } else {
            return Record.createIncome(user.getId(), aiResponse.amount(), aiResponse.description());
        }
    }

    public record AiResponse(BigDecimal amount, String description, Boolean error, RecordType type) {
    }

    public record MessageRequest(@NotBlank String from, @NotBlank @Size(min = 10) String message) {
    }
}
