package dev.matheuscruz.infra.ai.data;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Transactions;
import java.math.BigDecimal;

public record RecognizedTransaction(BigDecimal amount, String description, Transactions type, boolean withError,
        Categories category) {
}
