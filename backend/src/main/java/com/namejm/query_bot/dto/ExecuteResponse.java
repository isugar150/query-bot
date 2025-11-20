package com.namejm.query_bot.dto;

import java.util.List;

public record ExecuteResponse(
        List<String> columns,
        List<List<Object>> rows
) {
}
