/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.rhododendra.db;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.service.JSONLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MigrateJsonToDatabase {

    private static final Logger logger = LoggerFactory.getLogger(MigrateJsonToDatabase.class);

    private final JSONLoaderService jsonLoaderService;
    private final RhododendronRepository rhododendronRepository;
    private final HybridizerRepository hybridizerRepository;
    private final BotanistRepository botanistRepository;
    private final PhotoDetailsRepository photoDetailsRepository;
    private final Db db;

    public MigrateJsonToDatabase(
        JSONLoaderService jsonLoaderService,
        RhododendronRepository rhododendronRepository,
        HybridizerRepository hybridizerRepository,
        BotanistRepository botanistRepository,
        PhotoDetailsRepository photoDetailsRepository,
        Db db
    ) {
        this.jsonLoaderService = jsonLoaderService;
        this.rhododendronRepository = rhododendronRepository;
        this.hybridizerRepository = hybridizerRepository;
        this.botanistRepository = botanistRepository;
        this.photoDetailsRepository = photoDetailsRepository;
        this.db = db;
    }

    public void runMigration() throws IOException, SQLException {
        logger.info("Starting JSON → PostgreSQL migration");
        rebuildDatabaseForMigration();

        List<Botanist> botanists = jsonLoaderService.loadBotanists();
        Map<String, Long> botanistShortToId = botanists.stream()
            .collect(Collectors.toMap(Botanist::getBotanicalShort, botanist -> {
                try {
                    return botanistRepository.upsert(botanist);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        logger.info("Migrated {} botanists", botanists.size());

        List<PhotoDetails> photoDetails = jsonLoaderService.loadPhotoDetails();
        Map<String, Long> photoNameToId = photoDetails.stream()
            .collect(Collectors.toMap(PhotoDetails::getPhoto, photo -> {
                try {
                    return photoDetailsRepository.upsert(photo);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        logger.info("Migrated {} photo details", photoDetails.size());

        List<Hybridizer> hybridizers = jsonLoaderService.loadHybridizers();
        Map<String, Long> hybridizerOldIdToId = hybridizers.stream()
            .collect(Collectors.toMap(Hybridizer::getOldId, hybridizer -> {
                try {
                    return hybridizerRepository.upsert(hybridizer, photoNameToId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        logger.info("Migrated {} hybridizers", hybridizers.size());

        List<Rhododendron> rhodos = jsonLoaderService.loadRhodos();
        Map<String, Long> rhodoOldIdToId = rhodos.stream()
            .collect(Collectors.toMap(Rhododendron::getOldId, rhodo -> {
                try {
                    var h = rhodo.getHybridizer();
                    Long hybridizerId = (h == null || h.getHybridizerOldId() == null)
                        ? null
                        : hybridizerOldIdToId.get(h.getHybridizerOldId());

                    Rhododendron.RhodoKind kind = rhodo.computeRhodoKind();
                    rhodo.setRhodoKind(kind);

                    Integer introduced = parseIntroducedYear(rhodo);
                    rhodo.setIntroduced(introduced);

                    rhodo.setLepidote(Rhododendron.Lepidote.UNKNOWN);

                    return rhododendronRepository.upsert(rhodo, hybridizerId, photoNameToId, botanistShortToId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        for (Rhododendron r : rhodos) {
            rhododendronRepository.updateParentForeignKeys(r, rhodoOldIdToId);
        }
        logger.info("Migrated {} rhododendrons", rhodos.size());

        logger.info("JSON → PostgreSQL migration completed");
    }

    private static final Pattern TRAILING_YEAR = Pattern.compile("(\\d{4})\\s*$");

    static Integer parseIntroducedYear(Rhododendron rhodo) {
        if (rhodo.getCultivation_since() != null && !rhodo.getCultivation_since().isBlank()) {
            try {
                return Integer.parseInt(rhodo.getCultivation_since().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (rhodo.getHybridizer() != null && rhodo.getHybridizer().getHybridizer() != null) {
            Matcher m = TRAILING_YEAR.matcher(rhodo.getHybridizer().getHybridizer());
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return null;
    }

    private void rebuildDatabaseForMigration() throws SQLException {
        db.dropAllTables();
        db.createSchema();
    }
}
