package com.namejm.query_bot.dto;

import java.time.LocalDateTime;

public record ChatSessionSummary(
        Long id,
        Long dbId,
        String title,
        LocalDateTime createdAt,
        Long metabaseCardId,
        String metabaseCardUrl
) {
}
