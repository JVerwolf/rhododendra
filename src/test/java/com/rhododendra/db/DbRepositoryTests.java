package com.rhododendra.db;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "db.path=build/test-rhododendra.sqlite"
})
public class DbRepositoryTests {

    @Autowired
    private Db db;

    @Autowired
    private RhododendronRepository rhododendronRepository;

    @Autowired
    private BotanistRepository botanistRepository;

    @Autowired
    private PhotoDetailsRepository photoDetailsRepository;

    @Autowired
    private HybridizerRepository hybridizerRepository;

    @BeforeEach
    void cleanup() throws SQLException {
        try (var conn = db.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM rhododendron_botanical_synonym_botanist");
            stmt.executeUpdate("DELETE FROM rhododendron_botanical_synonym");
            stmt.executeUpdate("DELETE FROM rhododendron_first_described_botanist");
            stmt.executeUpdate("DELETE FROM rhododendron_synonym");
            stmt.executeUpdate("DELETE FROM rhododendron_photo");
            stmt.executeUpdate("DELETE FROM rhododendron");
            stmt.executeUpdate("DELETE FROM hybridizer");
            stmt.executeUpdate("DELETE FROM botanist");
            stmt.executeUpdate("DELETE FROM photo_details");
        }
    }

    @Test
    void testInsertAndLoadRhododendronWithRelations() throws SQLException {
        // Prepare related data
        var botanist = new Botanist();
        botanist.setBotanicalShort("B1");
        botanist.setFullName("Test Botanist");
        botanist.setLocation("Location");
        botanist.setBornDied("1900-1980");
        botanist.setImage("b1.jpg");
        botanistRepository.upsert(botanist);

        var photo = new PhotoDetails();
        photo.setPhoto("p1.jpg");
        photo.setPhotoBy("Photographer");
        photo.setDescription("Desc");
        photoDetailsRepository.upsert(photo);

        // Main rhododendron
        var rhodo = new Rhododendron();
        rhodo.setOldId("r1");
        rhodo.setName("Test Rhodo");
        rhodo.setPhotos(List.of("p1.jpg"));
        rhodo.setSynonyms(List.of("Synonym A", "Synonym B"));
        rhodo.setFirst_described_botanist_shorts(List.of("B1"));

        var syn = new Rhododendron.Synonym("Bot Syn 1", List.of("B1"));
        rhodo.setBotanical_synonyms(List.of(syn));

        Long rhodoId = rhododendronRepository.upsert(rhodo);

        var loaded = rhododendronRepository.getById(rhodoId);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Test Rhodo");
        assertThat(loaded.getPhotos()).containsExactly("p1.jpg");
        assertThat(loaded.getSynonyms()).containsExactly("Synonym A", "Synonym B");
        assertThat(loaded.getFirst_described_botanists()).extracting(Botanist::getBotanicalShort).containsExactly("B1");
        assertThat(loaded.getBotanical_synonyms()).hasSize(1);
        assertThat(loaded.getBotanical_synonyms().get(0).synonym()).isEqualTo("Bot Syn 1");
        assertThat(loaded.getBotanical_synonyms().get(0).botanical_shorts()).containsExactly("B1");
    }

    @Test
    void testRhododendronLoadsHybridizerNameFromHybridizerTable() throws SQLException {
        var hybridizer = new Hybridizer();
        hybridizer.setOldId("hz1");
        hybridizer.setName("Resolved Hybridizer Name");
        Long hybridizerId = hybridizerRepository.upsert(hybridizer);

        var inner = new Rhododendron.Hybridizer();
        inner.setHybridizerOldId("hz1");
        var rhodo = new Rhododendron();
        rhodo.setOldId("r-hz");
        rhodo.setName("With hybridizer");
        rhodo.setSpeciesOrCultivar(Rhododendron.SpeciesOrCultivar.CULTIVAR);
        rhodo.setHybridizer(inner);

        Long rhodoId = rhododendronRepository.upsert(rhodo, hybridizerId, null, null);

        var loaded = rhododendronRepository.getById(rhodoId);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getHybridizer()).isNotNull();
        assertThat(loaded.getHybridizer().getHybridizer_id()).isEqualTo(hybridizerId);
        assertThat(loaded.getHybridizer().getHybridizer()).isEqualTo("Resolved Hybridizer Name");
    }

    @Test
    void testUpdateEditableFields() throws SQLException {
        var rhodo = new Rhododendron();
        rhodo.setOldId("r-edit-fields");
        rhodo.setName("Editable");
        rhodo.setTen_year_height("2m");
        rhodo.setBloom_time("May");
        Long rhodoId = rhododendronRepository.upsert(rhodo);

        rhododendronRepository.updateEditableFields(
            rhodoId,
            "5m",       // tenYearHeight
            null,       // bloomTime
            null,       // flowerShape
            null,       // leafShape
            null,       // colour
            null,       // deciduous
            null,       // hardiness
            null,       // extraInformation
            null,       // additionalParentageInfo
            null,       // introduced (Integer)
            null,       // firstDescribed
            null,       // originLocation
            null,       // habit
            null,       // observedMatureHeight
            null,       // azaleaGroup
            null,       // irrcRegistered
            null,       // subgenus
            null,       // section
            null        // subsection
        );

        var loaded = rhododendronRepository.getById(rhodoId);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTen_year_height()).isEqualTo("5m");
        assertThat(loaded.getBloom_time()).isNull();
    }
}

