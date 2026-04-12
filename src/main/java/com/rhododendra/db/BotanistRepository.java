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

import com.rhododendra.model.Botanist;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class BotanistRepository {

    private final Db db;

    public BotanistRepository(Db db) {
        this.db = db;
    }

    public Long upsert(Botanist botanist) throws SQLException {
        var sql = """
            INSERT INTO botanist (botanical_short, full_name, location, born_died, image)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(botanical_short) DO UPDATE SET
                full_name = excluded.full_name,
                location = excluded.location,
                born_died = excluded.born_died,
                image = excluded.image
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, botanist.getBotanicalShort());
            ps.setString(2, botanist.getFullName());
            ps.setString(3, botanist.getLocation());
            ps.setString(4, botanist.getBornDied());
            ps.setString(5, botanist.getImage());
            ps.executeUpdate();
            Botanist stored = getByBotanicalShort(botanist.getBotanicalShort());
            if (stored == null || stored.getId() == null) {
                throw new SQLException("Botanist upsert failed for botanical_short=" + botanist.getBotanicalShort());
            }
            return stored.getId();
        }
    }

    public Botanist getById(Long id) throws SQLException {
        var sql = """
            SELECT id, botanical_short, full_name, location, born_died, image
            FROM botanist
            WHERE id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var b = new Botanist();
                b.setId(rs.getLong("id"));
                b.setBotanicalShort(rs.getString("botanical_short"));
                b.setFullName(rs.getString("full_name"));
                b.setLocation(rs.getString("location"));
                b.setBornDied(rs.getString("born_died"));
                b.setImage(rs.getString("image"));
                return b;
            }
        }
    }

    public Botanist getByBotanicalShort(String botanicalShort) throws SQLException {
        var sql = """
            SELECT id, botanical_short, full_name, location, born_died, image
            FROM botanist
            WHERE botanical_short = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, botanicalShort);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var b = new Botanist();
                b.setId(rs.getLong("id"));
                b.setBotanicalShort(rs.getString("botanical_short"));
                b.setFullName(rs.getString("full_name"));
                b.setLocation(rs.getString("location"));
                b.setBornDied(rs.getString("born_died"));
                b.setImage(rs.getString("image"));
                return b;
            }
        }
    }
}

