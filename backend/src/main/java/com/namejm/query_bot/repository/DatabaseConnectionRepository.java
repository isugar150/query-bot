package com.namejm.query_bot.repository;

import com.namejm.query_bot.domain.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {
}
