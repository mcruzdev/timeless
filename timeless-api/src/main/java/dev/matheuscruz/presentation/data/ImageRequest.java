package dev.matheuscruz.presentation.data;

import jakarta.validation.constraints.NotBlank;

public record ImageRequest(@NotBlank String from, @NotBlank String base64, String text, @NotBlank String mimeType) {
}
