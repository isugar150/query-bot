package com.namejm.query_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MetabaseQuestionRequest(
        @NotNull(message = "세션 ID는 필수입니다.") Long sessionId,
        @NotBlank(message = "쿼리를 입력하세요.") String query,
        String title,
        String description
) {
}
