package com.namejm.query_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExecuteRequest(
        @NotNull Long dbId,
        @NotBlank String sql
) {
}
