package com.rhododendra.db;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.service.JSONLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MigrateJsonToSqlite {

    private static final Logger logger = LoggerFactory.getLogger(MigrateJsonToSqlite.class);

    private final JSONLoaderService jsonLoaderService;
    private final RhododendronRepository rhododendronRepository;
    private final HybridizerRepository hybridizerRepository;
    private final BotanistRepository botanistRepository;
    private final PhotoDetailsRepository photoDetailsRepository;
    private final Db db;

    public MigrateJsonToSqlite(
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
        logger.info("Starting JSON → SQLite migration");
        rebuildDatabaseFile();

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

        List<Hybridizer> hybridizers = jsonLoaderService.loadHybridizers();
        Map<String, Long> hybridizerOldIdToId = hybridizers.stream()
            .collect(Collectors.toMap(Hybridizer::getOldId, hybridizer -> {
                try {
                    return hybridizerRepository.upsert(hybridizer);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        logger.info("Migrated {} hybridizers", hybridizers.size());

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

        List<Rhododendron> rhodos = jsonLoaderService.loadRhodos();
        Map<String, Long> rhodoOldIdToId = rhodos.stream()
            .collect(Collectors.toMap(Rhododendron::getOldId, rhodo -> {
                try {
                    var h = rhodo.getHybridizer();
                    Long hybridizerId = (h == null || h.getHybridizerOldId() == null)
                        ? null
                        : hybridizerOldIdToId.get(h.getHybridizerOldId());
                    return rhododendronRepository.upsert(rhodo, hybridizerId, photoNameToId, botanistShortToId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, (left, right) -> right));
        for (Rhododendron r : rhodos) {
            rhododendronRepository.updateParentForeignKeys(r, rhodoOldIdToId);
        }
        logger.info("Migrated {} rhododendrons", rhodos.size());

        logger.info("JSON → SQLite migration completed");
    }

    private void rebuildDatabaseFile() throws SQLException {
        String dbPath = System.getProperty("db.path");
        if (dbPath != null && !dbPath.isBlank() && !dbPath.startsWith(":")) {
            File dbFile = new File(dbPath);
            if (dbFile.exists() && !dbFile.delete()) {
                throw new SQLException("Could not delete database file: " + dbFile.getAbsolutePath());
            }
        }
        db.createSchema();
    }
}

