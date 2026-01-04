package dev.matheuscruz.presentation.data;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Transactions;
import java.math.BigDecimal;

public record UpdateRecordRequest(BigDecimal amount, String description, Transactions transaction, Categories category) {
}