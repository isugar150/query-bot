package com.namejm.query_bot.service;

import com.namejm.query_bot.domain.DatabaseConnection;
import com.namejm.query_bot.dto.ExecuteResponse;
import com.namejm.query_bot.repository.DatabaseConnectionRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryExecutionService {

    private static final Pattern READ_ONLY_PATTERN =
            Pattern.compile("^\\s*(select|with)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "^\\s*(insert|update|delete|create|alter|drop|truncate|merge|replace)\\b",
            Pattern.CASE_INSENSITIVE);

    private final DatabaseConnectionRepository databaseConnectionRepository;

    public QueryExecutionService(DatabaseConnectionRepository databaseConnectionRepository) {
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    @Transactional(readOnly = true)
    public ExecuteResponse executeSelect(Long dbId, String sql) throws Exception {
        DatabaseConnection db = databaseConnectionRepository.findById(dbId)
                .orElseThrow(() -> new IllegalArgumentException("데이터베이스 정보를 찾을 수 없습니다."));

        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!isReadOnly(trimmed)) {
            throw new IllegalArgumentException("데이터 조회 쿼리만 실행할 수 있습니다.");
        }

        try (Connection connection = openConnection(db);
             Statement stmt = connection.createStatement()) {
            // Limit result size without re-wrapping the query, so ORDER BY stays intact
            stmt.setMaxRows(100);
            try (ResultSet rs = stmt.executeQuery(trimmed)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }
                return new ExecuteResponse(columns, rows);
            }
        }
    }

    private Connection openConnection(DatabaseConnection db) throws Exception {
        String primaryDb = parseDatabaseName(db.getDatabaseName());
        String jdbcUrl = "jdbc:" + db.getDbType().getJdbcName() + "://" + db.getHost() + ":" + db.getPort() + "/" + primaryDb;
        Properties props = new Properties();
        props.setProperty("user", db.getUsername());
        props.setProperty("password", db.getPassword());
        props.setProperty("remarksReporting", "true");
        props.setProperty("useInformationSchema", "true");
        return DriverManager.getConnection(jdbcUrl, props);
    }

    private String parseDatabaseName(String raw) {
        int idx = raw.indexOf(',');
        if (idx < 0) {
            return raw.trim();
        }
        return raw.substring(0, idx).trim();
    }

    private boolean isReadOnly(String sql) {
        String[] statements = sql.split(";");
        boolean hasRead = false;
        for (String statement : statements) {
            String stmt = statement.trim();
            if (stmt.isEmpty()) {
                continue;
            }
            if (FORBIDDEN_PATTERN.matcher(stmt).find()) {
                return false;
            }
            if (!READ_ONLY_PATTERN.matcher(stmt).find()) {
                return false;
            }
            hasRead = true;
        }
        return hasRead;
    }
}
