package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
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

import static com.rhododendra.service.IndexService.BOTANIST_INDEX_PATH;
import static com.rhododendra.service.IndexService.SOURCE_KEY;

public class SearchService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Botanist> searchBotanists(String queryString) throws IOException, ParseException {
        return searchBotanists(queryString, Botanist.FULL_NAME_KEY);
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

    public static List<Botanist> getBotanistById(String id) throws IOException, ParseException {
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

}
