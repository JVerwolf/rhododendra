package com.rhododendra.db;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class Db implements InitializingBean {

    @Value("${db.path:./data/rhododendra.sqlite}")
    private String dbPath;

    public Connection getConnection() throws SQLException {
        ensureParentDirectoryExists(dbPath);
        var url = "jdbc:sqlite:" + dbPath;
        var conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private static void ensureParentDirectoryExists(String dbPath) throws SQLException {
        if (dbPath == null || dbPath.isBlank()) return;
        // Ignore SQLite special paths like ":memory:"
        if (dbPath.startsWith(":")) return;
        File file = new File(dbPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ok = parent.mkdirs();
            if (!ok && !parent.exists()) {
                throw new SQLException("Could not create database directory: " + parent.getAbsolutePath());
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection conn = getConnection()) {
            initializeSchema(conn);
        }
    }

    private void initializeSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Core tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hybridizer (
                    id TEXT PRIMARY KEY,
                    name TEXT,
                    location TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS botanist (
                    botanical_short TEXT PRIMARY KEY,
                    full_name TEXT,
                    location TEXT,
                    born_died TEXT,
                    image TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS photo_details (
                    photo TEXT PRIMARY KEY,
                    photo_by TEXT,
                    date TEXT,
                    location TEXT,
                    hi_res_photo TEXT,
                    description TEXT,
                    name TEXT,
                    tag TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron (
                    id TEXT PRIMARY KEY,
                    name TEXT,
                    species_or_cultivar TEXT,
                    is_species_selection INTEGER,
                    is_natural_hybrid INTEGER,
                    is_cultivar_group INTEGER,
                    rhodo_category TEXT,
                    ten_year_height TEXT,
                    bloom_time TEXT,
                    flower_shape TEXT,
                    leaf_shape TEXT,
                    hardiness TEXT,
                    deciduous TEXT,
                    colour TEXT,
                    extra_information TEXT,
                    irrc_registered TEXT,
                    additional_parentage_info TEXT,
                    species_id TEXT,
                    cultivation_since TEXT,
                    lepedote TEXT,
                    first_described TEXT,
                    origin_location TEXT,
                    habit TEXT,
                    observed_mature_height TEXT,
                    azalea_group TEXT,
                    subgenus TEXT,
                    section TEXT,
                    subsection TEXT,
                    seed_parent_id TEXT,
                    seed_parent_name TEXT,
                    pollen_parent_id TEXT,
                    pollen_parent_name TEXT,
                    hybridizer_id TEXT,
                    FOREIGN KEY (species_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (seed_parent_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (pollen_parent_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (hybridizer_id) REFERENCES hybridizer(id)
                )
                """);

            // Join tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_photo (
                    rhodo_id TEXT NOT NULL,
                    photo_id TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (photo_id) REFERENCES photo_details(photo)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hybridizer_photo (
                    hybridizer_id TEXT NOT NULL,
                    photo_id TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (hybridizer_id, pos),
                    FOREIGN KEY (hybridizer_id) REFERENCES hybridizer(id) ON DELETE CASCADE,
                    FOREIGN KEY (photo_id) REFERENCES photo_details(photo)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_synonym (
                    rhodo_id TEXT NOT NULL,
                    synonym TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_first_described_botanist (
                    rhodo_id TEXT NOT NULL,
                    botanical_short TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (botanical_short) REFERENCES botanist(botanical_short)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_botanical_synonym (
                    rhodo_id TEXT NOT NULL,
                    synonym TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_botanical_synonym_botanist (
                    rhodo_id TEXT NOT NULL,
                    synonym_pos INTEGER NOT NULL,
                    botanical_short TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, synonym_pos, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (botanical_short) REFERENCES botanist(botanical_short)
                )
                """);
        }
    }
}

