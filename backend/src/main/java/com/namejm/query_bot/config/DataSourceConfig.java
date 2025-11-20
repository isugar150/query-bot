package com.namejm.query_bot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @DependsOn("dataDirectoryInitializer")
    public DataSource dataSource(AppProperties properties, Environment environment) throws IOException {
        String explicitUrl = environment.getProperty("spring.datasource.url");
        String jdbcUrl;
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            jdbcUrl = explicitUrl;
        } else {
            Path dir = Path.of(properties.getDataDir());
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
                log.info("Created data directory at {}", dir.toAbsolutePath());
            }
            Path dbFile = dir.resolve("querybot.db");
            if (Files.notExists(dbFile)) {
                Files.createFile(dbFile);
                log.info("Created sqlite file at {}", dbFile.toAbsolutePath());
            }
            jdbcUrl = "jdbc:sqlite:" + dbFile;
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl(jdbcUrl);
        // SQLite allows a single writer; keep pool at 1 and set a small busy timeout to reduce lock errors.
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("busy_timeout", 5000);
        return new HikariDataSource(config);
    }
}
