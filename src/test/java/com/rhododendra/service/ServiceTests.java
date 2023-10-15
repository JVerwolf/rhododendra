package com.rhododendra.service;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybrid;
import com.rhododendra.model.Species;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.rhododendra.service.IndexService.*;
import static com.rhododendra.service.SearchService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceTests {


    @Test
    void testIndexAndSearchBotanists() throws IOException, ParseException {
        indexBotanists();
        assertThat(searchBotanists("Forrest")).isNotEmpty();
        assertEquals(1, getBotanistById("Forrest").size());
    }

    @Test
    void testIndexAndSearchSpecies() throws IOException, ParseException {
        indexSpecies();
        assertThat(searchSpecies("lemon")).isNotEmpty();
        assertEquals(1, getSpeciesById("s2373").size());
        assertThat(getAllSpeciesByFirstLetter("s")).hasSize(130);
    }

    @Test
    void testIndexAndSearchHybrids() throws IOException {
        indexHybrids();
        assertThat(searchHybrids("lemon")).isNotEmpty();
        assertEquals(1, getHybridById("h1").size());
        assertThat(getAllSpeciesByFirstLetter("s")).isNotEmpty();
    }

    @Test
    void testIndexAndSearchPhotoDetails() throws IOException, ParseException {
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
    void testLoadSpecies() throws IOException {
        List<Species> botanists = JSONLoaderService.loadSpecies();
        assertThat(botanists).isNotEmpty();
    }

    @Test
    void testLoadHybrid() throws IOException {
        try {
            List<Hybrid> hybrids = JSONLoaderService.loadHybrids();
            assertThat(hybrids).isNotEmpty();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    void testGetPageIndex() {
        assertThat(SearchService.getPageIndex(5,5,10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(2,5,10)).isEqualTo(0);
        assertThat(SearchService.getPageIndex(0,1,0)).isEqualTo(0);
        assertThat(SearchService.getPageIndex(9,5,10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(10,5,10)).isEqualTo(1);
        assertThat(SearchService.getPageIndex(1,1,1)).isEqualTo(0);
    }
}
