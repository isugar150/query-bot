package com.namejm.query_bot.dto;

public record DbTestResponse(
        boolean success,
        String message,
        SchemaOverview schema
) {
}
