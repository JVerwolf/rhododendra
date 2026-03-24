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

    public Long upsert(Hybridizer hybridizer) throws SQLException {
        var sql = """
            INSERT INTO hybridizer (old_id, name, location)
            VALUES (?, ?, ?)
            ON CONFLICT(old_id) DO UPDATE SET
                name = excluded.name,
                location = excluded.location
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, hybridizer.getOldId());
            ps.setString(2, hybridizer.getName());
            ps.setString(3, hybridizer.getLocation());
            ps.executeUpdate();
            Hybridizer stored = getByOldId(hybridizer.getOldId());
            if (stored == null || stored.getId() == null) {
                throw new SQLException("Hybridizer upsert failed for old_id=" + hybridizer.getOldId());
            }
            return stored.getId();
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
                // photos are handled via join table
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
                return h;
            }
        }
    }
}

