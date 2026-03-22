package com.rhododendra.controller;

import com.rhododendra.db.Db;
import com.rhododendra.db.RhododendronRepository;
import com.rhododendra.model.Rhododendron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "db.path=build/test-rhododendra-edit-post.sqlite"
})
class RhodoEditPostTests {

    private static final String RHODO_ID = "edit-post-r1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Db db;

    @Autowired
    private RhododendronRepository rhododendronRepository;

    @BeforeEach
    void setup() throws SQLException {
        try (var conn = db.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM rhododendron_botanical_synonym_botanist");
            stmt.executeUpdate("DELETE FROM rhododendron_botanical_synonym");
            stmt.executeUpdate("DELETE FROM rhododendron_first_described_botanist");
            stmt.executeUpdate("DELETE FROM rhododendron_synonym");
            stmt.executeUpdate("DELETE FROM rhododendron_photo");
            stmt.executeUpdate("DELETE FROM rhododendron");
        }

        var rhodo = new Rhododendron();
        rhodo.setId(RHODO_ID);
        rhodo.setName("Post Edit Test");
        rhodo.setTen_year_height("1m");
        rhododendronRepository.upsert(rhodo);
    }

    @Test
    void postEditWithoutAuthenticationRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/rhodos/" + RHODO_ID + "/edit")
                .with(csrf())
                .param("ten_year_height", "9m"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void postEditWithAuthenticationRedirectsAndUpdates() throws Exception {
        mockMvc.perform(post("/rhodos/" + RHODO_ID + "/edit")
                .with(csrf())
                .param("ten_year_height", "9m")
                .param("bloom_time", "")
                .param("flower_shape", "")
                .param("leaf_shape", "")
                .param("colour", "")
                .param("deciduous", "")
                .param("hardiness", "")
                .param("extra_information", "")
                .param("additional_parentage_info", "")
                .param("cultivation_since", "")
                .param("first_described", "")
                .param("origin_location", "")
                .param("habit", "")
                .param("observed_mature_height", "")
                .param("azalea_group", "")
                .param("irrc_registered", "")
                .param("subgenus", "")
                .param("section", "")
                .param("subsection", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/rhodos/" + RHODO_ID));

        var loaded = rhododendronRepository.getById(RHODO_ID);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTen_year_height()).isEqualTo("9m");
    }
}
