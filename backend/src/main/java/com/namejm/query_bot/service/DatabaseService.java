package com.namejm.query_bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.DbConnectionRequest;
import com.namejm.query_bot.dto.DbSummary;
import com.namejm.query_bot.dto.SchemaOverview;
import com.namejm.query_bot.repository.DatabaseConnectionRepository;
import com.namejm.query_bot.repository.ChatMessageRepository;
import com.namejm.query_bot.repository.ChatSessionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    private final DatabaseConnectionRepository repository;
    private final DatabaseMetadataService metadataService;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DatabaseService(DatabaseConnectionRepository repository, DatabaseMetadataService metadataService, ObjectMapper objectMapper,
                           ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository) {
        this.repository = repository;
        this.metadataService = metadataService;
        this.objectMapper = objectMapper;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<DbSummary> list() {
        return repository.findAll()
                .stream()
                .map(entity -> new DbSummary(
                        entity.getId(),
                        entity.getName(),
                        entity.getDbType(),
                        entity.getHost(),
                        entity.getPort(),
                        entity.getDatabaseName(),
                        entity.isSchemaReady()
                ))
                .toList();
    }

    public Optional<DatabaseConnection> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public DatabaseConnection register(DbConnectionRequest request, SchemaOverview schema) throws Exception {
        DatabaseConnection entity = new DatabaseConnection();
        entity.setName(request.name());
        entity.setDbType(request.dbType());
        entity.setHost(request.host());
        entity.setPort(request.port() != null ? request.port() : request.dbType().getDefaultPort());
        entity.setDatabaseName(request.databaseName());
        entity.setUsername(request.username());
        entity.setPassword(request.password());
        entity.setSchemaJson(objectMapper.writeValueAsString(schema));
        entity.setSchemaReady(true);
        entity.setSchemaUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(entity);
    }

    public SchemaOverview reloadSchema(DbConnectionRequest request) throws Exception {
        return metadataService.fetchAndThrow(request);
    }

    @Transactional
    public void deleteDatabase(Long id) {
        DatabaseConnection connection = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));
        var sessions = chatSessionRepository.findByDatabaseConnection(connection);
        if (!sessions.isEmpty()) {
            chatMessageRepository.deleteAllBySessionIn(sessions);
            chatSessionRepository.deleteAll(sessions);
        }
        repository.delete(connection);
    }
}
