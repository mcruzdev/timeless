package dev.matheuscruz.infra.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiTranscriptionResponse {
    @JsonProperty("text")
    public String text;
}
