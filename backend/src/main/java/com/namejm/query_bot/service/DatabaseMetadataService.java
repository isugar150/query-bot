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
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMetadataService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMetadataService.class);

    public DbTestResponse test(DbConnectionRequest request) {
        try (Connection connection = openConnection(request)) {
            SchemaOverview schema = extractSchema(connection, request.databaseName());
            String message = String.format("총 %d개 테이블을 발견했습니다.", schema.tables().size());
            return new DbTestResponse(true, message, schema);
        } catch (Exception ex) {
            log.warn("DB metadata 조회 실패: {}", ex.getMessage());
            return new DbTestResponse(false, ex.getMessage(), null);
        }
    }

    public SchemaOverview fetchAndThrow(DbConnectionRequest request) throws Exception {
        try (Connection connection = openConnection(request)) {
            return extractSchema(connection, request.databaseName());
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
        return "jdbc:" + type.getJdbcName() + "://" + request.host() + ":" + port + "/" + request.databaseName();
    }

    private SchemaOverview extractSchema(Connection connection, String databaseName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        List<TableOverview> tables = new ArrayList<>();
        try (ResultSet tableRs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tableRs.next()) {
                String tableName = tableRs.getString("TABLE_NAME");
                String tableComment = tableRs.getString("REMARKS");
                List<ColumnOverview> columns = new ArrayList<>();
                try (ResultSet columnRs = metaData.getColumns(null, null, tableName, "%")) {
                    while (columnRs.next()) {
                        String name = columnRs.getString("COLUMN_NAME");
                        String type = columnRs.getString("TYPE_NAME");
                        boolean nullable = "YES".equalsIgnoreCase(columnRs.getString("IS_NULLABLE"));
                        String comment = columnRs.getString("REMARKS");
                        columns.add(new ColumnOverview(name, type, nullable, comment));
                    }
                }
                tables.add(new TableOverview(tableName, columns, tableComment));
            }
        }
        return new SchemaOverview(databaseName, tables);
    }
}
