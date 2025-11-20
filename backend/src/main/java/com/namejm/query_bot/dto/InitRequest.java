package com.namejm.query_bot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InitRequest(
        @NotNull @Valid AdminCredentials admin,
        @Valid DbConnectionRequest database
) {
}
