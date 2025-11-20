package com.namejm.query_bot.dto;

import com.namejm.query_bot.model.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DbConnectionRequest(
        @NotBlank String name,
        @NotNull DatabaseType dbType,
        @NotBlank String host,
        Integer port,
        @NotBlank String databaseName,
        @NotBlank String username,
        @NotBlank String password
) {
}
