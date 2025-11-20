package com.namejm.query_bot.model;

public enum DatabaseType {
    MYSQL("mysql", 3306),
    MARIADB("mariadb", 3306),
    POSTGRESQL("postgresql", 5432);

    private final String jdbcName;
    private final int defaultPort;

    DatabaseType(String jdbcName, int defaultPort) {
        this.jdbcName = jdbcName;
        this.defaultPort = defaultPort;
    }

    public String getJdbcName() {
        return jdbcName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}
