package dev.matheuscruz.presentation.data;

public record UserInfoResponse(
        String id,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        Boolean hasPhoneNumber
) {
}
