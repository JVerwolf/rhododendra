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

package com.rhododendra.db;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
        createSchema();
    }

    public void createSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            initializeSchema(conn);
        }
    }

    private void initializeSchema(Connection conn) throws SQLException {
        if (requiresSchemaReset(conn)) {
            resetSchema(conn);
        }
        try (Statement stmt = conn.createStatement()) {
            // Core tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hybridizer (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    old_id TEXT UNIQUE,
                    name TEXT NOT NULL,
                    location TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS botanist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    botanical_short TEXT NOT NULL UNIQUE,
                    full_name TEXT,
                    location TEXT,
                    born_died TEXT,
                    image TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS photo_details (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    photo TEXT NOT NULL UNIQUE,
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
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    old_id TEXT UNIQUE,
                    name TEXT NOT NULL,
                    species_or_cultivar TEXT CHECK(species_or_cultivar IN ('CULTIVAR','SPECIES')),
                    is_species_selection INTEGER NOT NULL DEFAULT 0 CHECK(is_species_selection IN (0,1)),
                    is_natural_hybrid INTEGER NOT NULL DEFAULT 0 CHECK(is_natural_hybrid IN (0,1)),
                    is_cultivar_group INTEGER NOT NULL DEFAULT 0 CHECK(is_cultivar_group IN (0,1)),
                    rhodo_category TEXT CHECK(rhodo_category IN ('AZALEODENDRON','AZALEA','RHODO','VIREYA','UNKNOWN')),
                    rhodo_kind TEXT NOT NULL DEFAULT 'ARTIFICIAL_HYBRID'
                        CHECK(rhodo_kind IN ('SPECIES','NATURAL_HYBRID','ARTIFICIAL_HYBRID','SPECIES_SELECTION','CULTIVAR_GROUP')),
                    lepidote TEXT NOT NULL DEFAULT 'UNKNOWN'
                        CHECK(lepidote IN ('LEPIDOTE','ELEPIDOTE','UNKNOWN')),
                    introduced INTEGER,
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
                    species_id INTEGER,
                    first_described TEXT,
                    origin_location TEXT,
                    habit TEXT,
                    observed_mature_height TEXT,
                    azalea_group TEXT,
                    subgenus TEXT,
                    section TEXT,
                    subsection TEXT,
                    seed_parent_id INTEGER,
                    seed_parent_name TEXT,
                    pollen_parent_id INTEGER,
                    pollen_parent_name TEXT,
                    hybridizer_id INTEGER,
                    FOREIGN KEY (species_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (seed_parent_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (pollen_parent_id) REFERENCES rhododendron(id),
                    FOREIGN KEY (hybridizer_id) REFERENCES hybridizer(id),
                    CHECK(rhodo_kind NOT IN ('SPECIES','NATURAL_HYBRID') OR ten_year_height IS NULL),
                    CHECK(rhodo_kind NOT IN ('SPECIES','NATURAL_HYBRID') OR hybridizer_id IS NULL),
                    CHECK(azalea_group IS NULL
                          OR (rhodo_category = 'AZALEA'
                              AND rhodo_kind IN ('ARTIFICIAL_HYBRID','CULTIVAR_GROUP'))),
                    CHECK(rhodo_kind != 'SPECIES' OR is_cultivar_group = 0),
                    CHECK(rhodo_kind != 'SPECIES_SELECTION' OR is_cultivar_group = 0)
                )
                """);

            // Join tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_photo (
                    rhodo_id INTEGER NOT NULL,
                    photo_id INTEGER NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (photo_id) REFERENCES photo_details(id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hybridizer_photo (
                    hybridizer_id INTEGER NOT NULL,
                    photo_id INTEGER NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (hybridizer_id, pos),
                    FOREIGN KEY (hybridizer_id) REFERENCES hybridizer(id) ON DELETE CASCADE,
                    FOREIGN KEY (photo_id) REFERENCES photo_details(id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_synonym (
                    rhodo_id INTEGER NOT NULL,
                    synonym TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_first_described_botanist (
                    rhodo_id INTEGER NOT NULL,
                    botanist_id INTEGER NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (botanist_id) REFERENCES botanist(id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_botanical_synonym (
                    rhodo_id INTEGER NOT NULL,
                    synonym TEXT NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rhododendron_botanical_synonym_botanist (
                    rhodo_id INTEGER NOT NULL,
                    synonym_pos INTEGER NOT NULL,
                    botanist_id INTEGER NOT NULL,
                    pos INTEGER NOT NULL,
                    PRIMARY KEY (rhodo_id, synonym_pos, pos),
                    FOREIGN KEY (rhodo_id) REFERENCES rhododendron(id) ON DELETE CASCADE,
                    FOREIGN KEY (botanist_id) REFERENCES botanist(id)
                )
                """);
        }
    }

    private static boolean requiresSchemaReset(Connection conn) throws SQLException {
        if (!tableExists(conn, "rhododendron")) {
            return false;
        }
        return !columnExists(conn, "rhododendron", "old_id")
            || !columnExists(conn, "botanist", "id")
            || !columnExists(conn, "hybridizer", "old_id")
            || !columnExists(conn, "photo_details", "id")
            || !columnExists(conn, "rhododendron", "rhodo_kind")
            || !columnExists(conn, "rhododendron", "lepidote")
            || !columnExists(conn, "rhododendron", "introduced")
            || columnExists(conn, "rhododendron", "cultivation_since")
            || columnExists(conn, "rhododendron", "lepedote");
    }

    private static void resetSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS rhododendron_botanical_synonym_botanist");
            stmt.execute("DROP TABLE IF EXISTS rhododendron_botanical_synonym");
            stmt.execute("DROP TABLE IF EXISTS rhododendron_first_described_botanist");
            stmt.execute("DROP TABLE IF EXISTS rhododendron_synonym");
            stmt.execute("DROP TABLE IF EXISTS rhododendron_photo");
            stmt.execute("DROP TABLE IF EXISTS hybridizer_photo");
            stmt.execute("DROP TABLE IF EXISTS rhododendron");
            stmt.execute("DROP TABLE IF EXISTS hybridizer");
            stmt.execute("DROP TABLE IF EXISTS botanist");
            stmt.execute("DROP TABLE IF EXISTS photo_details");
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}

