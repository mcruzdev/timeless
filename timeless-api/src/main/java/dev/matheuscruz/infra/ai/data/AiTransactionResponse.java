package dev.matheuscruz.infra.ai.data;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.RecordType;
import java.math.BigDecimal;

public record AiTransactionResponse(String description, BigDecimal amount, RecordType type, boolean withError, Categories category) {
}
