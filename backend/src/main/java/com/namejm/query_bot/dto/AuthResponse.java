package com.namejm.query_bot.dto;

public record AuthResponse(
        String username,
        String accessToken,
        String refreshToken
) {
}
