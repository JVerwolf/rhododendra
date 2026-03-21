package com.rhododendra.db;

import com.rhododendra.model.Hybridizer;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class HybridizerRepository {

    private final Db db;

    public HybridizerRepository(Db db) {
        this.db = db;
    }

    public void upsert(Hybridizer hybridizer) throws SQLException {
        var sql = """
            INSERT INTO hybridizer (id, name, location)
            VALUES (?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                location = excluded.location
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, hybridizer.getId());
            ps.setString(2, hybridizer.getName());
            ps.setString(3, hybridizer.getLocation());
            ps.executeUpdate();
        }
    }

    public Hybridizer getById(String id) throws SQLException {
        var sql = "SELECT id, name, location FROM hybridizer WHERE id = ?";
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var h = new Hybridizer();
                h.setId(rs.getString("id"));
                h.setName(rs.getString("name"));
                h.setLocation(rs.getString("location"));
                // photos are handled via join table
                return h;
            }
        }
    }
}

