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

import com.rhododendra.model.Hybridizer;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class HybridizerRepository {

    private final Db db;

    public HybridizerRepository(Db db) {
        this.db = db;
    }

    public Long upsert(Hybridizer hybridizer) throws SQLException {
        return upsert(hybridizer, null);
    }

    public Long upsert(Hybridizer hybridizer, Map<String, Long> photoIdByName) throws SQLException {
        var sql = """
            INSERT INTO hybridizer (old_id, name, location)
            VALUES (?, ?, ?)
            ON CONFLICT(old_id) DO UPDATE SET
                name = excluded.name,
                location = excluded.location
            """;
        try (var conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, hybridizer.getOldId());
                ps.setString(2, hybridizer.getName());
                ps.setString(3, hybridizer.getLocation());
                ps.executeUpdate();
            }

            Long hybridizerId = selectIdByOldId(conn, hybridizer.getOldId());
            if (hybridizerId == null) {
                throw new SQLException("Hybridizer upsert failed for old_id=" + hybridizer.getOldId());
            }

            try (var del = conn.prepareStatement("DELETE FROM hybridizer_photo WHERE hybridizer_id = ?")) {
                del.setLong(1, hybridizerId);
                del.executeUpdate();
            }
            if (hybridizer.getPhotos() != null && !hybridizer.getPhotos().isEmpty()) {
                try (var ins = conn.prepareStatement(
                    "INSERT INTO hybridizer_photo (hybridizer_id, photo_id, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var photoName : hybridizer.getPhotos()) {
                        Long photoId = resolvePhotoId(conn, photoIdByName, photoName);
                        if (photoId == null) continue;
                        ins.setLong(1, hybridizerId);
                        ins.setLong(2, photoId);
                        ins.setInt(3, pos++);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            conn.commit();
            return hybridizerId;
        }
    }

    public Hybridizer getById(Long id) throws SQLException {
        var sql = "SELECT id, old_id, name, location FROM hybridizer WHERE id = ?";
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var h = new Hybridizer();
                h.setId(rs.getLong("id"));
                h.setOldId(rs.getString("old_id"));
                h.setName(rs.getString("name"));
                h.setLocation(rs.getString("location"));
                h.setPhotos(loadPhotos(conn, h.getId()));
                return h;
            }
        }
    }

    public Hybridizer getByOldId(String oldId) throws SQLException {
        var sql = "SELECT id, old_id, name, location FROM hybridizer WHERE old_id = ?";
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, oldId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var h = new Hybridizer();
                h.setId(rs.getLong("id"));
                h.setOldId(rs.getString("old_id"));
                h.setName(rs.getString("name"));
                h.setLocation(rs.getString("location"));
                h.setPhotos(loadPhotos(conn, h.getId()));
                return h;
            }
        }
    }

    private static Long selectIdByOldId(Connection conn, String oldId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT id FROM hybridizer WHERE old_id = ? LIMIT 1")) {
            ps.setString(1, oldId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static Long resolvePhotoId(Connection conn, Map<String, Long> photoIdByName, String photoName) throws SQLException {
        if (photoIdByName != null && photoIdByName.containsKey(photoName)) {
            return photoIdByName.get(photoName);
        }
        try (var ps = conn.prepareStatement("SELECT id FROM photo_details WHERE photo = ?")) {
            ps.setString(1, photoName);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static List<String> loadPhotos(Connection conn, Long id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT pd.photo FROM hybridizer_photo hp " +
                "JOIN photo_details pd ON pd.id = hp.photo_id " +
                "WHERE hp.hybridizer_id = ? ORDER BY hp.pos")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("photo"));
                }
            }
        }
        return list;
    }
}

