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

    public void upsert(PhotoDetails photo) throws SQLException {
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
        }
    }

    public PhotoDetails getById(String id) throws SQLException {
        var sql = """
            SELECT photo, photo_by, date, location, hi_res_photo, description, name, tag
            FROM photo_details
            WHERE photo = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var p = new PhotoDetails();
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

