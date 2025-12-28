package dev.matheuscruz.infra.ai;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;

@RegisterRestClient(baseUri = "https://api.openai.com/v1")
public interface OpenAiAudioApi {

    @POST
    @Path("/audio/transcriptions")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    OpenAiTranscriptionResponse transcribe(@HeaderParam("Authorization") String authorization,
            @MultipartForm OpenAiTranscriptionRequest request);
}
