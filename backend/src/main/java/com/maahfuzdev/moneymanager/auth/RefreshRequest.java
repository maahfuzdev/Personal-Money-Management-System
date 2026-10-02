package com.maahfuzdev.moneymanager.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        @Size(max = 128, message = "Refresh token is invalid")
        String refreshToken
) {
}
