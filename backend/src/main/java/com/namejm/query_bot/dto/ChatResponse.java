package com.namejm.query_bot.dto;

import java.util.List;

public record ChatResponse(
        Long sessionId,
        String reply,
        List<ChatMessageDto> history
) {
}
