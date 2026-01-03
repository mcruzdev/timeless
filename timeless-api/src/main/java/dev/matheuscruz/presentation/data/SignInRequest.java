package dev.matheuscruz.presentation.data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest(@Email String email, @NotBlank @Size(min = 8, max = 32) String password) {
}
