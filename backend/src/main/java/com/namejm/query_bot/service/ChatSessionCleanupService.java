package com.namejm.query_bot.service;

import com.namejm.query_bot.domain.ChatSession;
import com.namejm.query_bot.repository.ChatMessageRepository;
import com.namejm.query_bot.repository.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatSessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionCleanupService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSessionCleanupService(ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 4 * * *")
    public void deleteExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<ChatSession> expired = chatSessionRepository
                .findByLastQuestionAtBeforeOrLastQuestionAtIsNullAndCreatedAtBefore(cutoff, cutoff);

        if (expired.isEmpty()) {
            return;
        }

        chatMessageRepository.deleteAllBySessionIn(expired);
        chatSessionRepository.deleteAll(expired);
        log.info("Deleted {} chat sessions inactive since {}", expired.size(), cutoff);
    }
}
