package com.namejm.query_bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.DbConnectionRequest;
import com.namejm.query_bot.dto.DbSummary;
import com.namejm.query_bot.dto.SchemaOverview;
import com.namejm.query_bot.repository.DatabaseConnectionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    private final DatabaseConnectionRepository repository;
    private final DatabaseMetadataService metadataService;
    private final ObjectMapper objectMapper;

    public DatabaseService(DatabaseConnectionRepository repository, DatabaseMetadataService metadataService, ObjectMapper objectMapper) {
        this.repository = repository;
        this.metadataService = metadataService;
        this.objectMapper = objectMapper;
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
                        entity.getDatabaseName()
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
        return repository.save(entity);
    }

    public SchemaOverview reloadSchema(DbConnectionRequest request) throws Exception {
        return metadataService.fetchAndThrow(request);
    }
}
