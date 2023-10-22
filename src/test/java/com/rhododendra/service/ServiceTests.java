package com.rhododendra.service;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Rhododendron;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.rhododendra.service.IndexService.*;
import static com.rhododendra.service.SearchService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceTests {


    @Test
    void testIndexAndSearchBotanists() throws IOException {
        indexBotanists();
        assertThat(searchBotanists("Forrest")).isNotEmpty();
        assertEquals(1, getBotanistById("Forrest").size());
    }

    @Test
    void testIndexAndSearchRhodos() throws IOException {
        indexRhodos();
        assertThat(searchRhodos("lemon")).isNotEmpty();
        assertThat(searchRhodos("anna")).isNotEmpty();
        assertEquals(1, getRhodoById("h1").size());
        assertEquals(1, getRhodoById("s1").size());
        assertThat(getAllRhodosByFirstLetter("s", 10, 5).results).isNotEmpty();
    }

    @Test
    void testIndexAndSearchPhotoDetails() throws IOException {
        indexPhotoDetails();
        assertThat(searchPhotoDetails("Wedemire")).isNotEmpty();
        assertEquals(1, getPhotoDetailsById("s390_denudatum_17_normal.jpg").size());
        assertEquals(1, getPhotoDetailsById("h11579_Douglas_R_Stephens_1_normal.jpg").size());
    }


    @Test
    void testLoadBotanist() throws IOException {
        List<Botanist> botanists = JSONLoaderService.loadBotanists();
        assertThat(botanists).isNotEmpty();
    }

    @Test
    void testLoadRhodos() throws IOException {
        List<Rhododendron> rhodos = JSONLoaderService.loadRhodos();
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
}
