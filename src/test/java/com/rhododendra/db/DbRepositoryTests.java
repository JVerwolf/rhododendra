package com.rhododendra.db;

import com.rhododendra.model.Botanist;
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
        rhodo.setId("r1");
        rhodo.setName("Test Rhodo");
        rhodo.setPhotos(List.of("p1.jpg"));
        rhodo.setSynonyms(List.of("Synonym A", "Synonym B"));
        rhodo.setFirst_described_botanists(List.of("B1"));

        var syn = new Rhododendron.Synonym("Bot Syn 1", List.of("B1"));
        rhodo.setBotanical_synonyms(List.of(syn));

        rhododendronRepository.upsert(rhodo);

        var loaded = rhododendronRepository.getById("r1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Test Rhodo");
        assertThat(loaded.getPhotos()).containsExactly("p1.jpg");
        assertThat(loaded.getSynonyms()).containsExactly("Synonym A", "Synonym B");
        assertThat(loaded.getFirst_described_botanists()).containsExactly("B1");
        assertThat(loaded.getBotanical_synonyms()).hasSize(1);
        assertThat(loaded.getBotanical_synonyms().get(0).synonym()).isEqualTo("Bot Syn 1");
        assertThat(loaded.getBotanical_synonyms().get(0).botanical_shorts()).containsExactly("B1");
    }
}

