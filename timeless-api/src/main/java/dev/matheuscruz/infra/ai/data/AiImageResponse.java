package dev.matheuscruz.infra.ai.data;

import dev.matheuscruz.domain.RecordType;

import java.math.BigDecimal;

public record AiImageResponse(BigDecimal amount, String description, RecordType type, boolean withError) {

}
