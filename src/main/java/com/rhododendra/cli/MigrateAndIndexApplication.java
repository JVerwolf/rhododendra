package com.rhododendra.cli;

import com.rhododendra.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Headless entry point for migrating JSON → SQLite and rebuilding Lucene indexes.
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
