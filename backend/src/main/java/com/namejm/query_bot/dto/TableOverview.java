package com.namejm.query_bot.dto;

import java.util.List;

public record TableOverview(
        String name,
        List<ColumnOverview> columns,
        String comment
) {
}
