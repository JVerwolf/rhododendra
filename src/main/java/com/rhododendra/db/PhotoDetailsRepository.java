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

import com.rhododendra.model.PhotoDetails;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class PhotoDetailsRepository {

    private final Db db;

    public PhotoDetailsRepository(Db db) {
        this.db = db;
    }

    public Long upsert(PhotoDetails photo) throws SQLException {
        var sql = """
            INSERT INTO photo_details (photo, photo_by, date, location, hi_res_photo, description, name, tag)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(photo) DO UPDATE SET
                photo_by = excluded.photo_by,
                date = excluded.date,
                location = excluded.location,
                hi_res_photo = excluded.hi_res_photo,
                description = excluded.description,
                name = excluded.name,
                tag = excluded.tag
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, photo.getPhoto());
            ps.setString(2, photo.getPhotoBy());
            ps.setString(3, photo.getDate());
            ps.setString(4, photo.getLocation());
            ps.setString(5, photo.getHiResPhoto());
            ps.setString(6, photo.getDescription());
            ps.setString(7, photo.getName());
            ps.setString(8, photo.getTag());
            ps.executeUpdate();
            PhotoDetails stored = getByPhoto(photo.getPhoto());
            if (stored == null || stored.getId() == null) {
                throw new SQLException("PhotoDetails upsert failed for photo=" + photo.getPhoto());
            }
            return stored.getId();
        }
    }

    public PhotoDetails getById(Long id) throws SQLException {
        var sql = """
            SELECT id, photo, photo_by, date, location, hi_res_photo, description, name, tag
            FROM photo_details
            WHERE id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var p = new PhotoDetails();
                p.setId(rs.getLong("id"));
                p.setPhoto(rs.getString("photo"));
                p.setPhotoBy(rs.getString("photo_by"));
                p.setDate(rs.getString("date"));
                p.setLocation(rs.getString("location"));
                p.setHiResPhoto(rs.getString("hi_res_photo"));
                p.setDescription(rs.getString("description"));
                p.setName(rs.getString("name"));
                p.setTag(rs.getString("tag"));
                return p;
            }
        }
    }

    public PhotoDetails getByPhoto(String photo) throws SQLException {
        var sql = """
            SELECT id, photo, photo_by, date, location, hi_res_photo, description, name, tag
            FROM photo_details
            WHERE photo = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, photo);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var p = new PhotoDetails();
                p.setId(rs.getLong("id"));
                p.setPhoto(rs.getString("photo"));
                p.setPhotoBy(rs.getString("photo_by"));
                p.setDate(rs.getString("date"));
                p.setLocation(rs.getString("location"));
                p.setHiResPhoto(rs.getString("hi_res_photo"));
                p.setDescription(rs.getString("description"));
                p.setName(rs.getString("name"));
                p.setTag(rs.getString("tag"));
                return p;
            }
        }
    }
}

