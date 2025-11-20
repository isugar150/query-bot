package com.namejm.query_bot.controller;

import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.DbConnectionRequest;
import com.namejm.query_bot.dto.DbSummary;
import com.namejm.query_bot.dto.DbTestResponse;
import com.namejm.query_bot.dto.ExecuteRequest;
import com.namejm.query_bot.dto.ExecuteResponse;
import com.namejm.query_bot.service.DatabaseMetadataService;
import com.namejm.query_bot.service.DatabaseService;
import com.namejm.query_bot.service.QueryExecutionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db")
public class DatabaseController {

    private final DatabaseService databaseService;
    private final DatabaseMetadataService metadataService;
    private final QueryExecutionService queryExecutionService;

    public DatabaseController(DatabaseService databaseService, DatabaseMetadataService metadataService, QueryExecutionService queryExecutionService) {
        this.databaseService = databaseService;
        this.metadataService = metadataService;
        this.queryExecutionService = queryExecutionService;
    }

    @PostMapping("/test")
    public DbTestResponse test(@Valid @RequestBody DbConnectionRequest request) {
        return metadataService.test(request);
    }

    @PostMapping("/register")
    public DbSummary register(@Valid @RequestBody DbConnectionRequest request) throws Exception {
        var schema = databaseService.reloadSchema(request);
        DatabaseConnection saved = databaseService.register(request, schema);
        return new DbSummary(saved.getId(), saved.getName(), saved.getDbType(), saved.getHost(), saved.getPort(), saved.getDatabaseName(), saved.isSchemaReady());
    }

    @GetMapping("/list")
    public List<DbSummary> list() {
        return databaseService.list();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        databaseService.deleteDatabase(id);
    }

    @PostMapping("/execute")
    public ExecuteResponse execute(@Valid @RequestBody ExecuteRequest request) throws Exception {
        return queryExecutionService.executeSelect(request.dbId(), request.sql());
    }

    @PutMapping("/refresh/{id}")
    public DbSummary refresh(@PathVariable Long id) throws Exception {
        return databaseService.refresh(id);
    }
}
