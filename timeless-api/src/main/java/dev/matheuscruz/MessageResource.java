package dev.matheuscruz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;

@Path("/api/messages")
public class MessageResource {

    TimelessAiService aiService;
    ObjectMapper mapper;
    String assetsBucket;

    public MessageResource(
            TimelessAiService aiService,
            ObjectMapper mapper,
            @ConfigProperty(name = "assets.bucket") String assetsBucket
    ) {
        this.aiService = aiService;
        this.mapper = mapper;
        this.assetsBucket = assetsBucket;

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response message(MessageRequest req) {

        String json = aiService.identifyTransaction(req.transcription());

        try {
            AiResponse aiResponse = mapper.readValue(json, AiResponse.class);
            if (aiResponse.error()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                return Response.status(Response.Status.OK).entity(aiResponse).build();
            }
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public record AiResponse(BigDecimal amount, String description, Boolean error) {}
    public record MessageRequest(String location, String from, String transcription) {}
}
