package com.namejm.query_bot.dto;

public record ColumnOverview(
        String name,
        String type,
        boolean nullable,
        String comment
) {
}
