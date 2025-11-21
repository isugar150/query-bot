package com.namejm.query_bot.service;

import com.namejm.query_bot.dto.ColumnOverview;
import com.namejm.query_bot.dto.DbConnectionRequest;
import com.namejm.query_bot.dto.DbTestResponse;
import com.namejm.query_bot.dto.SchemaOverview;
import com.namejm.query_bot.dto.TableOverview;
import com.namejm.query_bot.model.DatabaseType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMetadataService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMetadataService.class);

    public DbTestResponse test(DbConnectionRequest request) {
        try (Connection connection = openConnection(request)) {
            SchemaOverview schema = extractSchema(connection, request);
            String message = String.format("총 %d개 테이블을 발견했습니다.", schema.tables().size());
            return new DbTestResponse(true, message, schema);
        } catch (Exception ex) {
            log.warn("DB metadata 조회 실패: {}", ex.getMessage());
            return new DbTestResponse(false, ex.getMessage(), null);
        }
    }

    public SchemaOverview fetchAndThrow(DbConnectionRequest request) throws Exception {
        try (Connection connection = openConnection(request)) {
            return extractSchema(connection, request);
        }
    }

    private Connection openConnection(DbConnectionRequest request) throws Exception {
        String jdbcUrl = buildJdbcUrl(request);
        Properties properties = new Properties();
        properties.setProperty("user", request.username());
        properties.setProperty("password", request.password());
        // Enable schema comments retrieval where supported
        properties.setProperty("remarksReporting", "true");
        properties.setProperty("useInformationSchema", "true");
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    private String buildJdbcUrl(DbConnectionRequest request) {
        int port = request.port() != null ? request.port() : request.dbType().getDefaultPort();
        DatabaseType type = request.dbType();
        String primaryDb = parseDatabaseName(request.databaseName());
        return "jdbc:" + type.getJdbcName() + "://" + request.host() + ":" + port + "/" + primaryDb;
    }

    private SchemaOverview extractSchema(Connection connection, DbConnectionRequest request) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String primaryDb = parseDatabaseName(request.databaseName());
        List<String> targetSchemas = resolveSchemas(request.dbType(), request.databaseName(), primaryDb);
        Set<String> discoveredSchemas = new LinkedHashSet<>();
        List<TableOverview> tables = new ArrayList<>();
        for (String schemaName : targetSchemas) {
            // For PostgreSQL: catalog is the database, schemaPattern can be null to fetch all; when schemaName is "%" we fetch all schemas.
            boolean fetchAllPgSchemas = request.dbType() == DatabaseType.POSTGRESQL && "%".equals(schemaName);
            String catalog = request.dbType() == DatabaseType.POSTGRESQL ? primaryDb : schemaName;
            String schemaPattern = request.dbType() == DatabaseType.POSTGRESQL
                    ? (fetchAllPgSchemas ? null : schemaName)
                    : null;

            try (ResultSet tableRs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String actualSchema = tableRs.getString("TABLE_SCHEM");
                    if (actualSchema == null || actualSchema.isBlank()) {
                        actualSchema = schemaName;
                    }
                    discoveredSchemas.add(actualSchema);
                    String tableName = tableRs.getString("TABLE_NAME");
                    String tableComment = tableRs.getString("REMARKS");
                    List<ColumnOverview> columns = new ArrayList<>();
                    try (ResultSet columnRs = metaData.getColumns(catalog, actualSchema, tableName, "%")) {
                        while (columnRs.next()) {
                            String name = columnRs.getString("COLUMN_NAME");
                            String type = columnRs.getString("TYPE_NAME");
                            boolean nullable = "YES".equalsIgnoreCase(columnRs.getString("IS_NULLABLE"));
                            String comment = columnRs.getString("REMARKS");
                            columns.add(new ColumnOverview(name, type, nullable, comment));
                        }
                    }
                    tables.add(new TableOverview(actualSchema, tableName, columns, tableComment));
                }
            }
        }
        List<String> schemasForResponse = discoveredSchemas.isEmpty() ? targetSchemas : new ArrayList<>(discoveredSchemas);
        return new SchemaOverview(primaryDb, schemasForResponse, tables);
    }

    private List<String> parseSchemas(String raw) {
        String[] parts = raw.split(",");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        if (tokens.isEmpty()) {
            tokens.add(raw.trim());
        }
        return tokens;
    }

    private String parseDatabaseName(String raw) {
        int idx = raw.indexOf(',');
        if (idx < 0) {
            return raw.trim();
        }
        return raw.substring(0, idx).trim();
    }

    private List<String> resolveSchemas(DatabaseType dbType, String rawDatabase, String primaryDb) {
        List<String> tokens = parseSchemas(rawDatabase);
        if (tokens.size() > 1) {
            return tokens.subList(1, tokens.size());
        }
        // No schemas explicitly provided.
        if (dbType == DatabaseType.POSTGRESQL) {
            // Default: fetch all schemas in the database.
            return List.of("%");
        }
        // For MySQL/MariaDB, schema == database.
        return List.of(primaryDb);
    }
}
