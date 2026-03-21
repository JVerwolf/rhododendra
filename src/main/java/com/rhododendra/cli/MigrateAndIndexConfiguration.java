package com.rhododendra.cli;

import com.rhododendra.db.MigrateJsonToSqlite;
import com.rhododendra.service.IndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.sql.SQLException;

@Configuration
@Profile("migrate")
public class MigrateAndIndexConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MigrateAndIndexConfiguration.class);

    @Bean
    public ApplicationRunner migrateAndIndexRunner(MigrateJsonToSqlite migrateJsonToSqlite) {
        return args -> {
            log.info("Starting migrate profile: JSON → SQLite, then Lucene reindex");
            try {
                migrateJsonToSqlite.runMigration();
            } catch (IOException | SQLException e) {
                log.error("Migration failed", e);
                throw new IllegalStateException("Migration failed", e);
            }

            try {
                IndexService.indexBotanists();
                IndexService.indexHybridizers();
                IndexService.indexPhotoDetails();
                IndexService.indexRhodos();
            } catch (IOException e) {
                log.error("Lucene indexing failed", e);
                throw new IllegalStateException("Indexing failed", e);
            }

            log.info("Migrate profile completed successfully");
        };
    }
}
