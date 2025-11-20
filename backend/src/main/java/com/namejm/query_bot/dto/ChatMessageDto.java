package com.namejm.query_bot.dto;

import com.namejm.query_bot.model.MessageRole;
import java.time.LocalDateTime;

public record ChatMessageDto(
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {
}
