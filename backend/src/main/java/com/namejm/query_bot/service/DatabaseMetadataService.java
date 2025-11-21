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
import java.sql.PreparedStatement;
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
            boolean isPostgres = request.dbType() == DatabaseType.POSTGRESQL;
            boolean fetchAllPgSchemas = isPostgres && "%".equals(schemaName);

            // Determine catalog/schemaPattern per DB type.
            String catalog;
            String schemaPattern;
            if (isPostgres) {
                catalog = primaryDb; // catalog is the database
                schemaPattern = fetchAllPgSchemas ? null : schemaName; // null => all schemas
            } else {
                // MySQL/MariaDB treat catalog as database; connection DB is primaryDb.
                catalog = schemaName;
                // If schemaName equals primaryDb, let schemaPattern be null; otherwise use schemaName to target another catalog.
                schemaPattern = null;
            }

            try (ResultSet tableRs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String actualSchema = tableRs.getString("TABLE_SCHEM");
                    if (actualSchema == null || actualSchema.isBlank()) {
                        actualSchema = schemaName;
                    }
                    discoveredSchemas.add(actualSchema);
                    String tableName = tableRs.getString("TABLE_NAME");
                    String tableComment = tableRs.getString("REMARKS");
                    List<ColumnOverview> columns = fetchColumns(request.dbType(), connection, catalog, actualSchema, tableName, metaData);
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
        // Always include all explicitly provided tokens as schema candidates (first token may also be a schema).
        if (tokens.size() > 1) {
            // Preserve order, avoid duplicates.
            return new ArrayList<>(new LinkedHashSet<>(tokens));
        }
        // No schemas explicitly provided.
        if (dbType == DatabaseType.POSTGRESQL) {
            // Default: fetch all schemas in the database.
            return List.of("%");
        }
        // For MySQL/MariaDB, schema == database.
        return List.of(primaryDb);
    }

    private List<ColumnOverview> fetchColumns(DatabaseType dbType, Connection connection, String catalog, String schema, String tableName, DatabaseMetaData metaData) throws Exception {
        // For PostgreSQL, prefer information_schema to avoid driver quirks and ensure full column lists.
        if (dbType == DatabaseType.POSTGRESQL) {
            String sql = """
                    select c.column_name,
                           c.data_type,
                           c.is_nullable,
                           pgd.description
                    from information_schema.columns c
                    join pg_catalog.pg_class cls on cls.relname = c.table_name
                    join pg_catalog.pg_namespace nsp on nsp.oid = cls.relnamespace and nsp.nspname = c.table_schema
                    left join pg_catalog.pg_description pgd on pgd.objoid = cls.oid and pgd.objsubid = c.ordinal_position
                    where c.table_schema = ? and c.table_name = ?
                    order by c.ordinal_position
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, schema);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ColumnOverview> columns = new ArrayList<>();
                    while (rs.next()) {
                        String name = rs.getString("column_name");
                        String type = rs.getString("data_type");
                        boolean nullable = "YES".equalsIgnoreCase(rs.getString("is_nullable"));
                        String comment = rs.getString("description");
                        columns.add(new ColumnOverview(name, type, nullable, comment));
                    }
                    if (!columns.isEmpty()) {
                        return columns;
                    }
                }
            }
            // Fallback if query returns nothing.
        }

        List<ColumnOverview> columns = new ArrayList<>();
        try (ResultSet columnRs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (columnRs.next()) {
                String name = columnRs.getString("COLUMN_NAME");
                String type = columnRs.getString("TYPE_NAME");
                boolean nullable = "YES".equalsIgnoreCase(columnRs.getString("IS_NULLABLE"));
                String comment = columnRs.getString("REMARKS");
                columns.add(new ColumnOverview(name, type, nullable, comment));
            }
        }
        return columns;
    }
}
