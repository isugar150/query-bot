package com.namejm.query_bot.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCredentials(
        @NotBlank String username,
        @NotBlank String password
) {
}
