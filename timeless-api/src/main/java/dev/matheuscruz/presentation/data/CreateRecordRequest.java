package dev.matheuscruz.presentation.data;

import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Transactions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecordRequest(@PositiveOrZero BigDecimal amount, @NotBlank String description,
        @NotNull Transactions transaction, @NotBlank String from, @NotNull Categories category,
        LocalDate transactionDate) {
}
