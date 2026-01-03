package dev.matheuscruz.presentation.data;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(@NotBlank String from, @NotBlank String message) {
}
