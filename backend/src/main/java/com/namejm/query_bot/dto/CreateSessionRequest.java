package com.namejm.query_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
        @NotNull Long dbId,
        @NotBlank String title
) {
}
