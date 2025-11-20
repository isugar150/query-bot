package com.namejm.query_bot.repository;

import com.namejm.query_bot.domain.ChatSession;
import com.namejm.query_bot.domain.DatabaseConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByDatabaseConnection(DatabaseConnection databaseConnection);
    Optional<ChatSession> findFirstByDatabaseConnectionOrderByCreatedAtDesc(DatabaseConnection databaseConnection);
}
