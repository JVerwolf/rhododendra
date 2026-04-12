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
import com.rhododendra.model.Rhododendron;
import com.rhododendra.model.Rhododendron.Parentage;
import com.rhododendra.model.Rhododendron.Synonym;
import com.rhododendra.model.Rhododendron.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class RhododendronRepository {

    private static final Logger log = LoggerFactory.getLogger(RhododendronRepository.class);

    private final Db db;

    public RhododendronRepository(Db db) {
        this.db = db;
    }

    public Long upsert(
        Rhododendron rhodo,
        Long hybridizerId,
        Map<String, Long> photoIdByName,
        Map<String, Long> botanistIdByShort
    ) throws SQLException {
        Rhododendron.RhodoKind kind = rhodo.getRhodoKind() != null
            ? rhodo.getRhodoKind()
            : rhodo.computeRhodoKind();
        rhodo.setRhodoKind(kind);

        boolean isWild = kind == Rhododendron.RhodoKind.SPECIES || kind == Rhododendron.RhodoKind.NATURAL_HYBRID;
        String tenYearHeight = isWild ? null : rhodo.getTen_year_height();
        Long effectiveHybridizerId = isWild ? null : hybridizerId;

        boolean isHybridAzalea = rhodo.getRhodoCategory() == Rhododendron.RhodoCategory.AZALEA
            && (kind == Rhododendron.RhodoKind.ARTIFICIAL_HYBRID || kind == Rhododendron.RhodoKind.CULTIVAR_GROUP);
        String azaleaGroup = isHybridAzalea ? rhodo.getAzalea_group() : null;

        String lepidoteStr = rhodo.getLepidote() == null ? "UNKNOWN" : rhodo.getLepidote().name();

        var sql = """
            INSERT INTO rhododendron (
                old_id, name, species_or_cultivar, is_species_selection, is_natural_hybrid, is_cultivar_group,
                rhodo_category, rhodo_kind, lepidote, introduced,
                ten_year_height, bloom_time, flower_shape, leaf_shape,
                hardiness, deciduous, colour, extra_information,
                irrc_registered, additional_parentage_info, species_id,
                first_described, origin_location, habit, observed_mature_height, azalea_group,
                subgenus, section, subsection,
                seed_parent_id, seed_parent_name, pollen_parent_id, pollen_parent_name,
                hybridizer_id
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(old_id) DO UPDATE SET
                name = excluded.name,
                species_or_cultivar = excluded.species_or_cultivar,
                is_species_selection = excluded.is_species_selection,
                is_natural_hybrid = excluded.is_natural_hybrid,
                is_cultivar_group = excluded.is_cultivar_group,
                rhodo_category = excluded.rhodo_category,
                rhodo_kind = excluded.rhodo_kind,
                lepidote = excluded.lepidote,
                introduced = excluded.introduced,
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
                int i = 1;
                ps.setString(i++, rhodo.getOldId());
                ps.setString(i++, rhodo.getName());
                ps.setString(i++, rhodo.getSpeciesOrCultivar() == null ? null : rhodo.getSpeciesOrCultivar().name());
                ps.setInt(i++, rhodo.getIs_species_selection() ? 1 : 0);
                ps.setInt(i++, rhodo.getIs_natural_hybrid() ? 1 : 0);
                ps.setInt(i++, rhodo.getIs_cultivar_group() ? 1 : 0);
                ps.setString(i++, rhodo.getRhodoCategory() == null ? null : rhodo.getRhodoCategory().name());
                ps.setString(i++, kind.name());
                ps.setString(i++, lepidoteStr);
                ps.setObject(i++, rhodo.getIntroduced());
                ps.setString(i++, tenYearHeight);
                ps.setString(i++, rhodo.getBloom_time());
                ps.setString(i++, rhodo.getFlower_shape());
                ps.setString(i++, rhodo.getLeaf_shape());
                ps.setString(i++, rhodo.getHardiness());
                ps.setString(i++, rhodo.getDeciduous());
                ps.setString(i++, rhodo.getColour());
                ps.setString(i++, rhodo.getExtra_information());
                ps.setString(i++, rhodo.getIrrc_registered());
                ps.setString(i++, rhodo.getAdditional_parentage_info());
                // Defer FKs to rhododendron(id): filled in updateParentForeignKeys() after all rows exist
                ps.setObject(i++, null);
                ps.setString(i++, rhodo.getFirst_described());
                ps.setString(i++, rhodo.getOrigin_location());
                ps.setString(i++, rhodo.getHabit());
                ps.setString(i++, rhodo.getObserved_mature_height());
                ps.setString(i++, azaleaGroup);

                Taxonomy taxonomy = rhodo.getTaxonomy();
                ps.setString(i++, taxonomy == null ? null : taxonomy.getSubgenus());
                ps.setString(i++, taxonomy == null ? null : taxonomy.getSection());
                ps.setString(i++, taxonomy == null ? null : taxonomy.getSubsection());

                Parentage parentage = rhodo.getParentage();
                ps.setObject(i++, null);
                ps.setString(i++, parentage == null ? null : parentage.getSeed_parent());
                ps.setObject(i++, null);
                ps.setString(i++, parentage == null ? null : parentage.getPollen_parent());
                ps.setObject(i, effectiveHybridizerId);

                ps.executeUpdate();
            }
            Long rhodoId = selectIdByOldId(conn, rhodo.getOldId());
            if (rhodoId == null) {
                throw new SQLException("Rhododendron upsert failed for old_id=" + rhodo.getOldId());
            }
            rhodo.setId(rhodoId);

            // Clear and re-insert list relationships
            try (var deletePhotos = conn.prepareStatement("DELETE FROM rhododendron_photo WHERE rhodo_id = ?");
                 var deleteSynonyms = conn.prepareStatement("DELETE FROM rhododendron_synonym WHERE rhodo_id = ?");
                 var deleteFirstDesc = conn.prepareStatement("DELETE FROM rhododendron_first_described_botanist WHERE rhodo_id = ?");
                 var deleteBotSyn = conn.prepareStatement("DELETE FROM rhododendron_botanical_synonym WHERE rhodo_id = ?");
                 var deleteBotSynBot = conn.prepareStatement("DELETE FROM rhododendron_botanical_synonym_botanist WHERE rhodo_id = ?")) {

                deletePhotos.setLong(1, rhodoId);
                deletePhotos.executeUpdate();
                deleteSynonyms.setLong(1, rhodoId);
                deleteSynonyms.executeUpdate();
                deleteFirstDesc.setLong(1, rhodoId);
                deleteFirstDesc.executeUpdate();
                deleteBotSyn.setLong(1, rhodoId);
                deleteBotSyn.executeUpdate();
                deleteBotSynBot.setLong(1, rhodoId);
                deleteBotSynBot.executeUpdate();
            }

            // photos
            if (rhodo.getPhotos() != null) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rhododendron_photo (rhodo_id, photo_id, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var photoName : rhodo.getPhotos()) {
                        Long photoId = resolvePhotoId(conn, photoIdByName, photoName);
                        if (photoId == null) continue;
                        insert.setLong(1, rhodoId);
                        insert.setLong(2, photoId);
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
                        insert.setLong(1, rhodoId);
                        insert.setString(2, syn);
                        insert.setInt(3, pos++);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            // first described botanists
            if (rhodo.getFirst_described_botanist_shorts() != null) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rhododendron_first_described_botanist (rhodo_id, botanist_id, pos) VALUES (?,?,?)")) {
                    int pos = 0;
                    for (var bShort : rhodo.getFirst_described_botanist_shorts()) {
                        Long botanistId = resolveBotanistId(conn, botanistIdByShort, bShort);
                        if (botanistId == null) continue;
                        insert.setLong(1, rhodoId);
                        insert.setLong(2, botanistId);
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
                         "INSERT INTO rhododendron_botanical_synonym_botanist (rhodo_id, synonym_pos, botanist_id, pos) VALUES (?,?,?,?)")) {
                    int synPos = 0;
                    for (Synonym syn : rhodo.getBotanical_synonyms()) {
                        insertSyn.setLong(1, rhodoId);
                        insertSyn.setString(2, syn.synonym());
                        insertSyn.setInt(3, synPos);
                        insertSyn.addBatch();

                        if (syn.botanical_shorts() != null) {
                            int bPos = 0;
                            for (String bShort : syn.botanical_shorts()) {
                                Long botanistId = resolveBotanistId(conn, botanistIdByShort, bShort);
                                if (botanistId == null) continue;
                                insertSynBot.setLong(1, rhodoId);
                                insertSynBot.setInt(2, synPos);
                                insertSynBot.setLong(3, botanistId);
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
            return rhodoId;
        }
    }

    public Long upsert(Rhododendron rhodo) throws SQLException {
        Long hybridizerId = null;
        if (rhodo.getHybridizer() != null && rhodo.getHybridizer().getHybridizerOldId() != null) {
            try (var conn = db.getConnection()) {
                hybridizerId = lookupHybridizerIdByOldId(conn, rhodo.getHybridizer().getHybridizerOldId());
            }
        }
        return upsert(rhodo, hybridizerId, null, null);
    }

    public void updateParentForeignKeys(Rhododendron rhodo, Map<String, Long> knownRhodoIds) throws SQLException {
        var sql = """
            UPDATE rhododendron
            SET species_id = ?, seed_parent_id = ?, pollen_parent_id = ?
            WHERE id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            Parentage p = rhodo.getParentage();
            Long speciesId = resolveRhodoFk(knownRhodoIds, rhodo.getOldId(), "species_id", rhodo.getSpeciesOldId());
            Long seedId = resolveRhodoFk(knownRhodoIds, rhodo.getOldId(), "seed_parent_id", p == null ? null : p.getSeedParentOldId());
            Long pollenId = resolveRhodoFk(knownRhodoIds, rhodo.getOldId(), "pollen_parent_id", p == null ? null : p.getPollenParentOldId());

            ps.setObject(1, speciesId);
            ps.setObject(2, seedId);
            ps.setObject(3, pollenId);
            ps.setLong(4, rhodo.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Updates descriptive/cultivation columns only. Blank strings clear nullable columns.
     * Does not touch photos, synonyms, parent FKs, or identity fields.
     */
    public int updateEditableFields(
        Long id,
        String tenYearHeight,
        String bloomTime,
        String flowerShape,
        String leafShape,
        String colour,
        String deciduous,
        String hardiness,
        String extraInformation,
        String additionalParentageInfo,
        Integer introduced,
        String firstDescribed,
        String originLocation,
        String habit,
        String observedMatureHeight,
        String azaleaGroup,
        String irrcRegistered,
        String subgenus,
        String section,
        String subsection
    ) throws SQLException {
        var sql = """
            UPDATE rhododendron SET
                ten_year_height = ?,
                bloom_time = ?,
                flower_shape = ?,
                leaf_shape = ?,
                colour = ?,
                deciduous = ?,
                hardiness = ?,
                extra_information = ?,
                additional_parentage_info = ?,
                introduced = ?,
                first_described = ?,
                origin_location = ?,
                habit = ?,
                observed_mature_height = ?,
                azalea_group = ?,
                irrc_registered = ?,
                subgenus = ?,
                section = ?,
                subsection = ?
            WHERE id = ?
            """;
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, blankToNull(tenYearHeight));
            ps.setString(i++, blankToNull(bloomTime));
            ps.setString(i++, blankToNull(flowerShape));
            ps.setString(i++, blankToNull(leafShape));
            ps.setString(i++, blankToNull(colour));
            ps.setString(i++, blankToNull(deciduous));
            ps.setString(i++, blankToNull(hardiness));
            ps.setString(i++, blankToNull(extraInformation));
            ps.setString(i++, blankToNull(additionalParentageInfo));
            ps.setObject(i++, introduced);
            ps.setString(i++, blankToNull(firstDescribed));
            ps.setString(i++, blankToNull(originLocation));
            ps.setString(i++, blankToNull(habit));
            ps.setString(i++, blankToNull(observedMatureHeight));
            ps.setString(i++, blankToNull(azaleaGroup));
            ps.setString(i++, blankToNull(irrcRegistered));
            ps.setString(i++, blankToNull(subgenus));
            ps.setString(i++, blankToNull(section));
            ps.setString(i++, blankToNull(subsection));
            ps.setLong(i, id);
            return ps.executeUpdate();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private Long resolveRhodoFk(
        Map<String, Long> knownRhodoIds,
        String rowId,
        String columnLabel,
        String refId
    ) {
        if (refId == null || refId.isBlank()) {
            return null;
        }
        Long resolved = knownRhodoIds.get(refId);
        if (resolved == null) {
            log.warn("Rhododendron {}: {}={} not found, storing null", rowId, columnLabel, refId);
            return null;
        }
        return resolved;
    }

    private static Long selectIdByOldId(java.sql.Connection conn, String oldId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT id FROM rhododendron WHERE old_id = ? LIMIT 1")) {
            ps.setString(1, oldId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static Long resolvePhotoId(java.sql.Connection conn, Map<String, Long> photoIdByName, String photoName) throws SQLException {
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

    private static Long resolveBotanistId(java.sql.Connection conn, Map<String, Long> botanistIdByShort, String botanicalShort) throws SQLException {
        if (botanistIdByShort != null && botanistIdByShort.containsKey(botanicalShort)) {
            return botanistIdByShort.get(botanicalShort);
        }
        try (var ps = conn.prepareStatement("SELECT id FROM botanist WHERE botanical_short = ?")) {
            ps.setString(1, botanicalShort);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static Long lookupHybridizerIdByOldId(java.sql.Connection conn, String oldId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT id FROM hybridizer WHERE old_id = ?")) {
            ps.setString(1, oldId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    public Rhododendron getById(Long id) throws SQLException {
        return getByColumn("r.id = ?", ps -> ps.setLong(1, id));
    }

    public Rhododendron getByOldId(String oldId) throws SQLException {
        return getByColumn("r.old_id = ?", ps -> ps.setString(1, oldId));
    }

    private interface StatementBinder {
        void bind(java.sql.PreparedStatement ps) throws SQLException;
    }

    private Rhododendron getByColumn(String whereClause, StatementBinder binder) throws SQLException {
        var sql = """
            SELECT r.*, hz.name AS hybridizer_name
            FROM rhododendron r
            LEFT JOIN hybridizer hz ON hz.id = r.hybridizer_id
            WHERE %s
            """;
        sql = sql.formatted(whereClause);
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var r = new Rhododendron();
                r.setId(rs.getLong("id"));
                r.setOldId(rs.getString("old_id"));
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
                var kindStr = rs.getString("rhodo_kind");
                if (kindStr != null) {
                    r.setRhodoKind(Rhododendron.RhodoKind.valueOf(kindStr));
                }
                var lepStr = rs.getString("lepidote");
                if (lepStr != null) {
                    r.setLepidote(Rhododendron.Lepidote.valueOf(lepStr));
                }
                var introducedObj = rs.getObject("introduced");
                if (introducedObj != null) {
                    r.setIntroduced(rs.getInt("introduced"));
                }

                r.setIrrc_registered(rs.getString("irrc_registered"));
                r.setAdditional_parentage_info(rs.getString("additional_parentage_info"));
                var speciesIdObj = rs.getObject("species_id");
                if (speciesIdObj != null) {
                    r.setSpecies_id(rs.getLong("species_id"));
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
                var seedParentIdObj = rs.getObject("seed_parent_id");
                if (seedParentIdObj != null) {
                    parentage.setSeed_parent_id(rs.getLong("seed_parent_id"));
                }
                parentage.setSeed_parent(rs.getString("seed_parent_name"));
                var pollenParentIdObj = rs.getObject("pollen_parent_id");
                if (pollenParentIdObj != null) {
                    parentage.setPollen_parent_id(rs.getLong("pollen_parent_id"));
                }
                parentage.setPollen_parent(rs.getString("pollen_parent_name"));
                if (parentage.getSeed_parent_id() != null || parentage.getPollen_parent_id() != null
                    || parentage.getSeed_parent() != null || parentage.getPollen_parent() != null) {
                    r.setParentage(parentage);
                }

                // hybridizer_id is on rhododendron; display name comes from hybridizer table (JSON name is not stored redundantly)
                var hybridizerIdObj = rs.getObject("hybridizer_id");
                if (hybridizerIdObj != null) {
                    var inner = new Rhododendron.Hybridizer();
                    inner.setHybridizer_id(rs.getLong("hybridizer_id"));
                    inner.setHybridizer(rs.getString("hybridizer_name"));
                    r.setHybridizer(inner);
                }

                // lists
                r.setPhotos(loadPhotos(conn, r.getId()));
                r.setSynonyms(loadSimpleSynonyms(conn, r.getId()));
                r.setFirst_described_botanists(loadFirstDescribedBotanists(conn, r.getId()));
                r.setBotanical_synonyms(loadBotanicalSynonyms(conn, r.getId()));

                return r;
            }
        }
    }

    private List<String> loadPhotos(java.sql.Connection conn, Long id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT pd.photo FROM rhododendron_photo rp JOIN photo_details pd ON pd.id = rp.photo_id WHERE rp.rhodo_id = ? ORDER BY rp.pos")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("photo"));
                }
            }
        }
        return list;
    }

    private List<String> loadSimpleSynonyms(java.sql.Connection conn, Long id) throws SQLException {
        var list = new ArrayList<String>();
        try (var ps = conn.prepareStatement(
            "SELECT synonym FROM rhododendron_synonym WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("synonym"));
                }
            }
        }
        return list;
    }

    private List<Botanist> loadFirstDescribedBotanists(java.sql.Connection conn, Long id) throws SQLException {
        var list = new ArrayList<Botanist>();
        try (var ps = conn.prepareStatement(
            "SELECT b.id, b.botanical_short, b.full_name, b.location, b.born_died, b.image " +
                "FROM rhododendron_first_described_botanist rfdb " +
                "JOIN botanist b ON b.id = rfdb.botanist_id " +
                "WHERE rfdb.rhodo_id = ? ORDER BY rfdb.pos")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var botanist = new Botanist();
                    botanist.setId(rs.getLong("id"));
                    botanist.setBotanicalShort(rs.getString("botanical_short"));
                    botanist.setFullName(rs.getString("full_name"));
                    botanist.setLocation(rs.getString("location"));
                    botanist.setBornDied(rs.getString("born_died"));
                    botanist.setImage(rs.getString("image"));
                    list.add(botanist);
                }
            }
        }
        return list;
    }

    private List<Synonym> loadBotanicalSynonyms(java.sql.Connection conn, Long id) throws SQLException {
        var list = new ArrayList<Synonym>();
        try (var ps = conn.prepareStatement(
            "SELECT synonym, pos FROM rhododendron_botanical_synonym WHERE rhodo_id = ? ORDER BY pos")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var synonym = rs.getString("synonym");
                    var synPos = rs.getInt("pos");
                    var shorts = new ArrayList<String>();
                    try (var ps2 = conn.prepareStatement(
                        "SELECT b.botanical_short FROM rhododendron_botanical_synonym_botanist rbsb " +
                            "JOIN botanist b ON b.id = rbsb.botanist_id " +
                            "WHERE rbsb.rhodo_id = ? AND rbsb.synonym_pos = ? ORDER BY rbsb.pos")) {
                        ps2.setLong(1, id);
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

