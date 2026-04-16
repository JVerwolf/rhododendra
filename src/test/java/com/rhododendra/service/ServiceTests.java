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

package com.rhododendra.service;

import com.rhododendra.AbstractPostgresSpringBootTest;
import com.rhododendra.db.MigrateJsonToDatabase;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Rhododendron;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static com.rhododendra.service.IndexService.*;
import static com.rhododendra.service.SearchService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceTests extends AbstractPostgresSpringBootTest {

    @Autowired
    JSONLoaderService jsonLoaderService;

    @Autowired
    MigrateJsonToDatabase migrateJsonToDatabase;

    @BeforeAll
    void beforeAll() throws IOException {
        try {
            migrateJsonToDatabase.runMigration();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        indexBotanists();
        indexHybridizers();
        indexPhotoDetails();
        indexRhodos();
    }

    @Test
    void testIndexAndSearchBotanists() {
        assertThat(searchBotanists("Forrest")).isNotEmpty();
        assertEquals(1, getBotanistByBotanicalShort("Forrest").size());
    }

    @Test
    void testIndexAndSearchRhodos() throws IOException, ParseException {
        assertThat(searchRhodos("lemon", 1, 0).results).isNotEmpty();
        assertThat(searchRhodos("anna", 1, 0).results).isNotEmpty();
        assertEquals(1, getRhodoByOldId("h1").size());
        assertEquals(1, getRhodoByOldId("s1").size());
        assertThat(getAllRhodosByFirstLetter("a", 10, 0, false, null).results).isNotEmpty();
    }

    @Test
    void testIndexAndSearchPhotoDetails() {
        assertThat(searchPhotoDetails("Wedemire")).isNotEmpty();
        assertEquals(1, getPhotoDetailsByPhoto("s390_denudatum_17_normal.jpg").size());
        assertEquals(1, getPhotoDetailsByPhoto("h11579_Douglas_R_Stephens_1_normal.jpg").size());
    }

    @Test
    void testIndexAndSearchHybridizers() throws IOException, ParseException {
        assertEquals(1, getHybridizerByOldId("p303").size());
        assertThat(searchHybridizers("Rothschild",10, 0).results).isNotEmpty();

//        assertEquals(1, getPhotoDetailsById("h11579_Douglas_R_Stephens_1_normal.jpg").size());
    }


    @Test
    void testLoadBotanist() throws IOException {
        List<Botanist> botanists = jsonLoaderService.loadBotanists();
        assertThat(botanists).isNotEmpty();
    }

    @Test
    void testLoadRhodos() throws IOException {
        List<Rhododendron> rhodos = jsonLoaderService.loadRhodos();
        assertThat(rhodos).isNotEmpty();
    }


    @Test
    void testGetPageIndex() {
        assertThat(SearchService.getPageIndex(5, 5, 10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(2, 5, 10)).isEqualTo(0);
        assertThat(SearchService.getPageIndex(0, 1, 0)).isEqualTo(0);
        assertThat(SearchService.getPageIndex(9, 5, 10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(10, 5, 10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(1, 1, 1)).isEqualTo(0);
    }

    @Test
    void testGetAllRhodoIds() {
        assertThat(SearchService.getAllRhodoIds()).isNotNull();
    }
}
