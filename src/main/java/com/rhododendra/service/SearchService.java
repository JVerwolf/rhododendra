package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Species;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.rhododendra.service.IndexService.*;

public class SearchService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Botanist> searchBotanists(String queryString) throws IOException, ParseException {
        return searchBotanists(queryString, Botanist.FULL_NAME_KEY);
    }

    public static List<Species> searchSpecies(String queryString) throws IOException, ParseException {
        return searchSpecies(queryString, Species.NAME_KEY);
    }

    private static List<Botanist> searchBotanists(String queryString, String inField) throws IOException, ParseException {
        Query query = new QueryParser(inField, new StandardAnalyzer()).parse(queryString);
        Directory indexDirectory = FSDirectory.open(Paths.get(BOTANIST_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);

        List<Botanist> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {

            var test = searcher.doc(scoreDoc.doc);
            for (var field : test.getFields(SOURCE_KEY)) {
                var botanist = objectMapper.readValue(field.stringValue(), new TypeReference<Botanist>() {
                });
                searchResults.add(botanist);
            }
        }
        return searchResults;
    }

    public static List<Botanist> getBotanistById(String id) throws IOException {
        Query query = new TermQuery(new Term(Botanist.PRIMARY_ID_KEY, id));
        Directory indexDirectory = FSDirectory.open(Paths.get(BOTANIST_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 1);

        List<Botanist> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            var test = searcher.doc(scoreDoc.doc);
            for (var field : test.getFields(SOURCE_KEY)) {
                var botanist = objectMapper.readValue(
                    field.stringValue(),
                    new TypeReference<Botanist>() {
                    });
                searchResults.add(botanist);
            }
        }
        return searchResults;
    }

    private static List<Species> searchSpecies(String queryString, String inField) throws IOException, ParseException {
        Query query = new QueryParser(inField, new StandardAnalyzer()).parse(queryString);
        Directory indexDirectory = FSDirectory.open(Paths.get(SPECIES_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);

        List<Species> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {

            var test = searcher.doc(scoreDoc.doc);
            for (var field : test.getFields(SOURCE_KEY)) {
                var species = objectMapper.readValue(field.stringValue(), new TypeReference<Species>() {
                });
                searchResults.add(species);
            }
        }
        return searchResults;
    }

    public static List<Species> getSpeciesById(String id) throws IOException {
        Query query = new TermQuery(new Term(Species.PRIMARY_ID_KEY, id));
        Directory indexDirectory = FSDirectory.open(Paths.get(SPECIES_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 1);

        List<Species> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            var test = searcher.doc(scoreDoc.doc);
            for (var field : test.getFields(SOURCE_KEY)) {
                var species = objectMapper.readValue(
                    field.stringValue(),
                    new TypeReference<Species>() {
                    });
                searchResults.add(species);
            }
        }
        return searchResults;
    }
}
