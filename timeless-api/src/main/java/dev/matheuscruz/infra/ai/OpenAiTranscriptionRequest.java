package dev.matheuscruz.infra.ai;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import org.jboss.resteasy.reactive.PartType;

public class OpenAiTranscriptionRequest {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;

    @FormParam("model")
    @PartType(MediaType.TEXT_PLAIN)
    public String model;
}
