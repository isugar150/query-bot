package com.namejm.query_bot.repository;

import com.namejm.query_bot.domain.ChatMessage;
import com.namejm.query_bot.domain.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    void deleteAllBySession(ChatSession session);
    void deleteAllBySessionIn(List<ChatSession> sessions);
}
