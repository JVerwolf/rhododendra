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

package com.rhododendra.controller;

import com.rhododendra.AbstractPostgresSpringBootTest;
import com.rhododendra.db.Db;
import com.rhododendra.db.RhododendronRepository;
import com.rhododendra.model.Rhododendron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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
class RhodoEditPostTests extends AbstractPostgresSpringBootTest {

    private static final String RHODO_OLD_ID = "edit-post-r1";
    private Long rhodoId;

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
        rhodo.setOldId(RHODO_OLD_ID);
        rhodo.setName("Post Edit Test");
        rhodo.setTen_year_height("1m");
        rhodoId = rhododendronRepository.upsert(rhodo);
    }

    @Test
    void postEditWithoutAuthenticationRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/rhodos/" + rhodoId + "/edit")
                .with(csrf())
                .param("ten_year_height", "9m"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void postEditWithAuthenticationRedirectsAndUpdates() throws Exception {
        mockMvc.perform(post("/rhodos/" + rhodoId + "/edit")
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
                .param("introduced", "")
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
            .andExpect(redirectedUrl("/rhodos/" + rhodoId));

        var loaded = rhododendronRepository.getById(rhodoId);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTen_year_height()).isEqualTo("9m");
    }
}
