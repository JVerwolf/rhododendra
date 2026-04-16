/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.rhododendra;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * Starts an embedded PostgreSQL (Zonky) for integration tests — no Docker required.
 * For CI with Docker, you can alternatively use Testcontainers; this setup matches
 * production PostgreSQL behavior without extra infrastructure.
 */
public abstract class AbstractPostgresSpringBootTest {

    private static final EmbeddedPostgres EMBEDDED;

    static {
        try {
            EMBEDDED = EmbeddedPostgres.builder().setPort(0).start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                EMBEDDED.close();
            } catch (Exception ignored) {
            }
        }));
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EMBEDDED.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }
}
