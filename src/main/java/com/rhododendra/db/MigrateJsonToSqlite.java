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
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MigrateJsonToSqlite {

    private static final Logger logger = LoggerFactory.getLogger(MigrateJsonToSqlite.class);

    private final JSONLoaderService jsonLoaderService;
    private final RhododendronRepository rhododendronRepository;
    private final HybridizerRepository hybridizerRepository;
    private final BotanistRepository botanistRepository;
    private final PhotoDetailsRepository photoDetailsRepository;

    public MigrateJsonToSqlite(
        JSONLoaderService jsonLoaderService,
        RhododendronRepository rhododendronRepository,
        HybridizerRepository hybridizerRepository,
        BotanistRepository botanistRepository,
        PhotoDetailsRepository photoDetailsRepository
    ) {
        this.jsonLoaderService = jsonLoaderService;
        this.rhododendronRepository = rhododendronRepository;
        this.hybridizerRepository = hybridizerRepository;
        this.botanistRepository = botanistRepository;
        this.photoDetailsRepository = photoDetailsRepository;
    }

    public void runMigration() throws IOException, SQLException {
        logger.info("Starting JSON → SQLite migration");

        List<Botanist> botanists = jsonLoaderService.loadBotanists();
        for (Botanist b : botanists) {
            botanistRepository.upsert(b);
        }
        logger.info("Migrated {} botanists", botanists.size());

        List<Hybridizer> hybridizers = jsonLoaderService.loadHybridizers();
        for (Hybridizer h : hybridizers) {
            hybridizerRepository.upsert(h);
        }
        logger.info("Migrated {} hybridizers", hybridizers.size());

        List<PhotoDetails> photoDetails = jsonLoaderService.loadPhotoDetails();
        for (PhotoDetails p : photoDetails) {
            photoDetailsRepository.upsert(p);
        }
        logger.info("Migrated {} photo details", photoDetails.size());

        List<Rhododendron> rhodos = jsonLoaderService.loadRhodos();
        var rhodoIds = rhodos.stream().map(Rhododendron::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (Rhododendron r : rhodos) {
            rhododendronRepository.upsert(r);
        }
        for (Rhododendron r : rhodos) {
            rhododendronRepository.updateParentForeignKeys(r, rhodoIds);
        }
        logger.info("Migrated {} rhododendrons", rhodos.size());

        logger.info("JSON → SQLite migration completed");
    }
}

