package com.maahfuzdev.moneymanager.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
}
