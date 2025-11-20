package com.namejm.query_bot.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataDirectoryInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataDirectoryInitializer.class);

    private final AppProperties appProperties;

    public DataDirectoryInitializer(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void ensureDataDir() throws IOException {
        Path path = Path.of(appProperties.getDataDir());
        if (Files.notExists(path)) {
            Files.createDirectories(path);
            log.info("Created data directory at {}", path.toAbsolutePath());
        }
        Path dbFile = path.resolve("querybot.db");
        if (Files.notExists(dbFile)) {
            Files.createFile(dbFile);
            log.info("Created sqlite file at {}", dbFile.toAbsolutePath());
        }
    }
}
