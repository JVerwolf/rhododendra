package com.rhododendra.service;

import com.rhododendra.model.Botanist;
import com.rhododendra.model.Species;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.rhododendra.service.IndexService.indexBotanists;
import static com.rhododendra.service.IndexService.indexSpecies;
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
        assertThat(searchSpecies("microphyton")).isNotEmpty();
        assertEquals(1, getSpeciesById("5").size());
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
}

