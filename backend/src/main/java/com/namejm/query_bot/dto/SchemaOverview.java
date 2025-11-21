package com.namejm.query_bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SchemaOverview(
        String database,
        List<String> schemas,
        List<TableOverview> tables
) {
}
