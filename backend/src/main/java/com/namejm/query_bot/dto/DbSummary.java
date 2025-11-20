package com.namejm.query_bot.dto;

import com.namejm.query_bot.model.DatabaseType;

public record DbSummary(
        Long id,
        String name,
        DatabaseType dbType,
        String host,
        Integer port,
        String databaseName
) {
}
