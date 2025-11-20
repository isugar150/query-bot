package com.namejm.query_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotNull Long dbId,
        Long sessionId,
        @NotBlank String message
) {
}
