package dev.matheuscruz.infra.ai;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AudioAiService {

    @RestClient
    OpenAiAudioApi openAiAudioApi;

    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    String apiKey;

    public String transcribe(String base64Audio) {
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);

        OpenAiTranscriptionRequest request = new OpenAiTranscriptionRequest();
        request.file = new ByteArrayInputStream(audioBytes);
        request.model = "whisper-1";

        OpenAiTranscriptionResponse response = openAiAudioApi.transcribe("Bearer " + apiKey, request);
        return response.text;
    }
}
