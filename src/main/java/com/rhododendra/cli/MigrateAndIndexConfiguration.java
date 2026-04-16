/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.rhododendra.cli;

import com.rhododendra.db.MigrateJsonToDatabase;
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
    public ApplicationRunner migrateAndIndexRunner(MigrateJsonToDatabase migrateJsonToDatabase) {
        return args -> {
            log.info("Starting migrate profile: JSON → PostgreSQL, then Lucene reindex");
            try {
                migrateJsonToDatabase.runMigration();
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
