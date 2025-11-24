package com.namejm.query_bot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "database_connection_id")
    private DatabaseConnection databaseConnection;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    private Long systemPromptDatabaseId;

    private Long metabaseCardId;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Long getSystemPromptDatabaseId() {
        return systemPromptDatabaseId;
    }

    public void setSystemPromptDatabaseId(Long systemPromptDatabaseId) {
        this.systemPromptDatabaseId = systemPromptDatabaseId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getMetabaseCardId() {
        return metabaseCardId;
    }

    public void setMetabaseCardId(Long metabaseCardId) {
        this.metabaseCardId = metabaseCardId;
    }
}
