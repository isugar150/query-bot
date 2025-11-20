package com.namejm.query_bot.dto;

import java.util.List;

public record SchemaOverview(
        String database,
        List<TableOverview> tables
) {
}
