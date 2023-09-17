package com.rhododendra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.rhododendra.service.IndexService.indexBotanists;
import static com.rhododendra.service.IndexService.souceFieldType;
import static com.rhododendra.service.SearchService.searchBotanists;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceTests {



    @Test
    void testIndexAndSearchBotanists() throws IOException, ParseException {
        indexBotanists();
        var botanists = searchBotanists("full_name", "Forrest");
        assertThat(botanists).isNotEmpty();
    }


    @Test
    void testReadBotanist() throws IOException {
        List<Botanist> botanists = JSONLoaderService.readBotanists();
        assertThat(botanists).isNotEmpty();
    }
}

