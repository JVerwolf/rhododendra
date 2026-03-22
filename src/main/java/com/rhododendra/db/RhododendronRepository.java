package com.rhododendra.db;

import com.rhododendra.model.Rhododendron;
import com.rhododendra.model.Rhododendron.Parentage;
import com.rhododendra.model.Rhododendron.Synonym;
import com.rhododendra.model.Rhododendron.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

@Repository
public class RhododendronRepository {

    private static final Logger log = LoggerFactory.getLogger(RhododendronRepository.class);

    private final Db db;

    public RhododendronRepository(Db db) {
        this.db = db;
    }

    public void upsert(Rhododendron rhodo) throws SQLException {
        var sql = """
            INSERT INTO rhododendron (
                id, name, species_or_cultivar, is_species_selection, is_natural_hybrid, is_cultivar_group,
                rhodo_category, ten_year_height, bloom_time, flower_shape, leaf_shape,
                hardiness, deciduous, colour, extra_information,
                irrc_registered, additional_parentage_info, species_id, cultivation_since, lepedote,
                first_described, origin_location, habit, observed_mature_height, azalea_group,
                subgenus, section, subsection,
                seed_parent_id, seed_parent_name, pollen_parent_id, pollen_parent_name,
                hybridizer_id
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                species_or_cultivar = excluded.species_or_cultivar,
                is_species_selection = excluded.is_species_selection,
                is_natural_hybrid = excluded.is_natural_hybrid,
                is_cultivar_group = excluded.is_cultivar_group,
                rhodo_category = excluded.rhodo_category,
                ten_year_height = excluded.ten_year_height,
                bloom_time = excluded.bloom_time,
                flower_shape = excluded.flower_shape,
                leaf_shape = excluded.leaf_shape,
                hardiness = excluded.hardiness,
                deciduous = excluded.deciduous,
                colour = excluded.colour,
                extra_information = excluded.extra_information,
                irrc_registered = excluded.irrc_registered,
                additional_parentage_info = excluded.additional_parentage_info,
                cultivation_since = excluded.cultivation_since,
                lepedote = excluded.lepedote,
                first_described = excluded.first_described,
                origin_location = excluded.origin_location,
                habit = excluded.habit,
                observed_mature_height = excluded.observed_mature_height,
                azalea_group = excluded.azalea_group,
                subgenus = excluded.subgenus,
                section = excluded.section,
                subsection = excluded.subsection,
                seed_parent_name = excluded.seed_parent_name,
                pollen_parent_name = excluded.pollen_parent_name,
                hybridizer_id = excluded.hybridizer_id
            """;

        try (var conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, rhodo.getId());
                ps.setString(2, rhodo.getName());
                ps.setString(3, rhodo.getSpeciesOrCultivar() == null ? null : rhodo.getSpeciesOrCultivar().name());
                ps.setInt(4, rhodo.getIs_species_selection() ? 1 : 0);
                ps.setInt(5, rhodo.getIs_natural_hybrid() ? 1 : 0);
                ps.setInt(6, rhodo.getIs_cultivar_group() ? 1 : 0);
                ps.setString(7, rhodo.getRhodoCategory() == null ? null : rhodo.getRhodoCategory().name());
                ps.setString(8, rhodo.getTen_year_height());
                ps.setString(9, rhodo.getBloom_time());
                ps.setString(10, rhodo.getFlower_shape());
                ps.setString(11, rhodo.getLeaf_shape());
                ps.setString(12, rhodo.getHardiness());
                ps.setString(13, rhodo.getDeciduous());
                ps.setString(14, rhodo.getColour());
                ps.setString(15, rhodo.getExtra_information());
                ps.setString(16, rhodo.getIrrc_registered());
                ps.setString(17, rhodo.getAdditional_parentage_info());
                // Defer FKs to rhododendron(id): filled in updateParentForeignKeys() after all rows exist
                ps.setString(18, null);
                ps.setString(19, rhodo.getCultivation_since());
                ps.setString(20, rhodo.getLepedote() == null ? null : rhodo.getLepedote().name());
                ps.setString(21, rhodo.getFirst_described());
                ps.setString(22, rhodo.getOrigin_location());
                ps.setString(23, rhodo.getHabit());
                ps.setString(24, rhodo.getObserved_mature_height());
                ps.setString(25, rhodo.getAzalea_group());

                Taxonomy taxonomy = rhodo.getTaxonomy();
                ps.setString(26, taxonomy == null ? null : taxonomy.getSubgenus());
                ps.setString(27, taxonomy == null ? null : taxonomy.getSection());
                ps.setString(28, taxonomy == null ? null : taxonomy.getSubsection());

                Parentage parentage = rhodo.getParentage();
                ps.setString(29, null);
                ps.setString(30, parentage == null ? null : parentage.getSeed_parent());
                ps.setString(31, null);
                ps.setString(32, parentage == null ? null : parentage.getPollen_parent());

                var hybridizer = rhodo.getHybridizer();
                ps.setString(33, hybridizer == null ? null : hybridizer.getHybridizer_id());

                ps.executeUpdate();
            }

            // Clear and re-insert list relationships
            try (var deletePhotos = conn.prepareStatement("DELETE FROM rhododendron_photo WHERE rhodo_id = ?");
                 var deleteSynonyms = conn.prepareStatement("DELETE FROM rhododendron_synonym WHERE rhodo_id = ?");
                 var deleteFirstDesc = conn.prepareStatement("DELETE FROM rhododendron_first_described_botanist WHERE rhodo_id = ?");
                 var deleteBotSyn = conn.prepareStatement("DELETE FROM rhododendron_botanical_synonym WHERE rhodo_id = ?");
                 var deleteBotSynBot = conn.prepareStatement("DELETE FROM rhododendron_botanical_synonym_botanist WHERE rhodo_id = ?")) {

                deletePhotos.setString(1, rhodo.getId());
                deletePhotos.executeUpdate();
                deleteSynonyms.setString(1, rhodo.getId());
                deleteSynonyms.executeUpdate();
                deleteFirstDesc.setString(1, rhodo.getId());
                deleteFirstDesc.executeUpdate();
                deleteBotSyn.setString(1, rhodo.getId());
                deleteBotSyn.executeUpdate();
                deleteBotSynBot.setString(1, rhodo.getId());
                deleteBotSynBot.executeUpdate();
            }

            // photos
            if (rhodo.getPhotos() != null) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rhododendron_photo (rhodo_id, photo_id, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var photoId : rhodo.getPhotos()) {
                        insert.setString(1, rhodo.getId());
                        insert.setString(2, photoId);
                        insert.setInt(3, pos++);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            // simple synonyms
            if (rhodo.getSynonyms() != null) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rhododendron_synonym (rhodo_id, synonym, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var syn : rhodo.getSynonyms()) {
                        insert.setString(1, rhodo.getId());
                        insert.setString(2, syn);
                        insert.setInt(3, pos++);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            // first described botanists
            if (rhodo.getFirst_described_botanists() != null) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rhododendron_first_described_botanist (rhodo_id, botanical_short, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var b : rhodo.getFirst_described_botanists()) {
                        insert.setString(1, rhodo.getId());
                        insert.setString(2, b);
                        insert.setInt(3, pos++);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            // botanical synonyms with nested botanists
            if (rhodo.getBotanical_synonyms() != null) {
                try (var insertSyn = conn.prepareStatement(
                         "INSERT INTO rhododendron_botanical_synonym (rhodo_id, synonym, pos) VALUES (?,?,?)");
                     var insertSynBot = conn.prepareStatement(
                         "INSERT INTO rhododendron_botanical_synonym_botanist (rhodo_id, synonym_pos, botanical_short, pos) VALUES (?,?,?,?)")) {
                    int synPos = 0;
                    for (Synonym syn : rhodo.getBotanical_synonyms()) {
                        insertSyn.setString(1, rhodo.getId());
                        insertSyn.setString(2, syn.synonym());
                        insertSyn.setInt(3, synPos);
                        insertSyn.addBatch();

                        if (syn.botanical_shorts() != null) {
                            int bPos = 0;
                            for (String bShort : syn.botanical_shorts()) {
                                insertSynBot.setString(1, rhodo.getId());
                                insertSynBot.setInt(2, synPos);
                                insertSynBot.setString(3, bShort);
                                insertSynBot.setInt(4, bPos++);
                                insertSynBot.addBatch();
                            }
                        }
                        synPos++;
                    }
                    insertSyn.executeBatch();
                    insertSynBot.executeBatch();
                }
            }

            conn.commit();
        }
    }

    public void updateParentForeignKeys(Rhododendron rhodo, Set<String> knownRhodoIds) throws SQLException {
        var sql = """
            UPDATE rhododendron
            SET species_id = ?, seed_parent_id = ?, pollen_parent_id = ?
            WHERE id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            Parentage p = rhodo.getParentage();
            String speciesId = normalizeFk(rhodo.getSpecies_id());
            String seedId = p == null ? null : normalizeFk(p.getSeed_parent_id());
            String pollenId = p == null ? null : normalizeFk(p.getPollen_parent_id());

            speciesId = resolveRhodoFk(conn, knownRhodoIds, rhodo.getId(), "species_id", speciesId);
            seedId = resolveRhodoFk(conn, knownRhodoIds, rhodo.getId(), "seed_parent_id", seedId);
            pollenId = resolveRhodoFk(conn, knownRhodoIds, rhodo.getId(), "pollen_parent_id", pollenId);

            ps.setString(1, speciesId);
            ps.setString(2, seedId);
            ps.setString(3, pollenId);
            ps.setString(4, rhodo.getId());
            ps.executeUpdate();
        }
    }

    private static String normalizeFk(String id) {
        return id == null || id.isBlank() ? null : id;
    }

    private String resolveRhodoFk(
        java.sql.Connection conn,
        Set<String> knownRhodoIds,
        String rowId,
        String columnLabel,
        String refId
    ) throws SQLException {
        if (refId == null) {
            return null;
        }
        boolean ok = knownRhodoIds != null
            ? knownRhodoIds.contains(refId)
            : rhodoRowExists(conn, refId);
        if (!ok) {
            log.warn("Rhododendron {}: {}={} not found, storing null", rowId, columnLabel, refId);
            return null;
        }
        return refId;
    }

    private static boolean rhodoRowExists(java.sql.Connection conn, String id) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT 1 FROM rhododendron WHERE id = ? LIMIT 1")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Rhododendron getById(String id) throws SQLException {
        var sql = """
            SELECT r.*, hz.name AS hybridizer_name
            FROM rhododendron r
            LEFT JOIN hybridizer hz ON hz.id = r.hybridizer_id
            WHERE r.id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var r = new Rhododendron();
                r.setId(rs.getString("id"));
                r.setName(rs.getString("name"));
                var soc = rs.getString("species_or_cultivar");
                if (soc != null) {
                    r.setSpeciesOrCultivar(Rhododendron.SpeciesOrCultivar.valueOf(soc));
                }
                r.setIs_species_selection(rs.getInt("is_species_selection") == 1);
                r.setIs_natural_hybrid(rs.getInt("is_natural_hybrid") == 1);
                r.setIs_cultivar_group(rs.getInt("is_cultivar_group") == 1);
                var rc = rs.getString("rhodo_category");
                if (rc != null) {
                    r.setRhodoCategory(Rhododendron.RhodoCategory.valueOf(rc));
                }
                r.setTen_year_height(rs.getString("ten_year_height"));
                r.setBloom_time(rs.getString("bloom_time"));
                r.setFlower_shape(rs.getString("flower_shape"));
                r.setLeaf_shape(rs.getString("leaf_shape"));
                r.setHardiness(rs.getString("hardiness"));
                r.setDeciduous(rs.getString("deciduous"));
                r.setColour(rs.getString("colour"));
                r.setExtra_information(rs.getString("extra_information"));
                r.setIrrc_registered(rs.getString("irrc_registered"));
                r.setAdditional_parentage_info(rs.getString("additional_parentage_info"));
                r.setSpecies_id(rs.getString("species_id"));
                r.setCultivation_since(rs.getString("cultivation_since"));
                var lep = rs.getString("lepedote");
                if (lep != null) {
                    r.setLepedote(Rhododendron.Lepedote.valueOf(lep));
                }
                r.setFirst_described(rs.getString("first_described"));
                r.setOrigin_location(rs.getString("origin_location"));
                r.setHabit(rs.getString("habit"));
                r.setObserved_mature_height(rs.getString("observed_mature_height"));
                r.setAzalea_group(rs.getString("azalea_group"));

                var taxonomy = new Taxonomy();
                taxonomy.setSubgenus(rs.getString("subgenus"));
                taxonomy.setSection(rs.getString("section"));
                taxonomy.setSubsection(rs.getString("subsection"));
                if (taxonomy.getSubgenus() != null || taxonomy.getSection() != null || taxonomy.getSubsection() != null) {
                    r.setTaxonomy(taxonomy);
                }

                var parentage = new Parentage();
                parentage.setSeed_parent_id(rs.getString("seed_parent_id"));
                parentage.setSeed_parent(rs.getString("seed_parent_name"));
                parentage.setPollen_parent_id(rs.getString("pollen_parent_id"));
                parentage.setPollen_parent(rs.getString("pollen_parent_name"));
                if (parentage.getSeed_parent_id() != null || parentage.getPollen_parent_id() != null
                    || parentage.getSeed_parent() != null || parentage.getPollen_parent() != null) {
                    r.setParentage(parentage);
                }

                // hybridizer_id is on rhododendron; display name comes from hybridizer table (JSON name is not stored redundantly)
                var hybridizerId = rs.getString("hybridizer_id");
                if (hybridizerId != null) {
                    var inner = new Rhododendron.Hybridizer();
                    inner.setHybridizer_id(hybridizerId);
                    inner.setHybridizer(rs.getString("hybridizer_name"));
                    r.setHybridizer(inner);
                }

                // lists
                r.setPhotos(loadPhotos(conn, id));
                r.setSynonyms(loadSimpleSynonyms(conn, id));
                r.setFirst_described_botanists(loadFirstDescribedBotanists(conn, id));
                r.setBotanical_synonyms(loadBotanicalSynonyms(conn, id));

                return r;
            }
        }
    }

    private java.util.List<String> loadPhotos(java.sql.Connection conn, String id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT photo_id FROM rhododendron_photo WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("photo_id"));
                }
            }
        }
        return list;
    }

    private java.util.List<String> loadSimpleSynonyms(java.sql.Connection conn, String id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT synonym FROM rhododendron_synonym WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("synonym"));
                }
            }
        }
        return list;
    }

    private java.util.List<String> loadFirstDescribedBotanists(java.sql.Connection conn, String id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT botanical_short FROM rhododendron_first_described_botanist WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("botanical_short"));
                }
            }
        }
        return list;
    }

    private java.util.List<Synonym> loadBotanicalSynonyms(java.sql.Connection conn, String id) throws SQLException {
        var list = new ArrayList<Synonym>();
        try (var ps = conn.prepareStatement(
            "SELECT synonym, pos FROM rhododendron_botanical_synonym WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var synonym = rs.getString("synonym");
                    var synPos = rs.getInt("pos");
                    var shorts = new ArrayList<String>();
                    try (var ps2 = conn.prepareStatement(
                        "SELECT botanical_short FROM rhododendron_botanical_synonym_botanist WHERE rhodo_id = ? AND synonym_pos = ? ORDER BY pos")) {
                        ps2.setString(1, id);
                        ps2.setInt(2, synPos);
                        try (var rs2 = ps2.executeQuery()) {
                            while (rs2.next()) {
                                shorts.add(rs2.getString("botanical_short"));
                            }
                        }
                    }
                    list.add(new Synonym(synonym, shorts));
                }
            }
        }
        return list;
    }
}

