package com.rhododendra.service;

import com.rhododendra.model.Botanist;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.rhododendra.service.IndexService.indexBotanists;
import static com.rhododendra.service.SearchService.getBotanistById;
import static com.rhododendra.service.SearchService.searchBotanists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceTests {

    @BeforeAll
    void setup() throws IOException {
        indexBotanists();
    }

    @Test
    void testIndexAndSearchBotanists() throws IOException, ParseException {
        assertThat(searchBotanists("Forrest")).isNotEmpty();
        assertEquals(1, getBotanistById("Forrest").size());
    }


    @Test
    void testReadBotanist() throws IOException {
        List<Botanist> botanists = JSONLoaderService.readBotanists();
        assertThat(botanists).isNotEmpty();
    }
}

