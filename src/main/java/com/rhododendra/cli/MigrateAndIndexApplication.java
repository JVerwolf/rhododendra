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

import com.rhododendra.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Headless entry point for migrating JSON → PostgreSQL and rebuilding Lucene indexes.
 * Run via: {@code ./gradlew migrateAndIndex}
 */
public final class MigrateAndIndexApplication {

    private MigrateAndIndexApplication() {
    }

    public static void main(String[] args) {
        int exitCode = SpringApplication.exit(
            new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .profiles("migrate")
                .run(args)
        );
        System.exit(exitCode);
    }
}
