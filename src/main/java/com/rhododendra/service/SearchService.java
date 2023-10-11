package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybrid;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Species;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rhododendra.service.IndexService.*;

public class SearchService {
    private static Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Botanist> searchBotanists(String queryString) {
        try {
            return search(
                new QueryParser(Botanist.FULL_NAME_KEY, new StandardAnalyzer()).parse(queryString),
                BOTANIST_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search searchBotanists", e);
            return Collections.emptyList();
        }
    }

    public static List<Species> searchSpecies(String queryString) {
        try {
            return search(
                new QueryParser(Species.NAME_KEY, new StandardAnalyzer()).parse(queryString),
                SPECIES_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search searchSpecies", e);
            return Collections.emptyList();
        }
    }

    public static List<Hybrid> searchHybrids(String queryString) {
        try {
            return search(
                new QueryParser(Hybrid.NAME_KEY, new StandardAnalyzer()).parse(queryString),
                HYBRIDS_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search searchHybrid", e);
            return Collections.emptyList();
        }
    }

    public static List<PhotoDetails> searchPhotoDetails(String queryString) {
        try {
            return search(
                new QueryParser(PhotoDetails.PHOTO_BY, new StandardAnalyzer()).parse(queryString),
                PHOTO_DETAIL_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search searchPhotoDetails", e);
            return Collections.emptyList();
        }
    }

    public static List<Botanist> getBotanistById(String id) {
        try {
            return search(
                new TermQuery(new Term(Botanist.PRIMARY_ID_KEY, id)),
                BOTANIST_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search getBotanistById", e);
            return Collections.emptyList();
        }
    }

    public static List<Species> getSpeciesById(String id) {
        try {
            return search(
                new TermQuery(new Term(Species.PRIMARY_ID_KEY, id)),
                SPECIES_INDEX_PATH,
                new TypeReference<Species>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search getSpeciesById", e);
            return Collections.emptyList();
        }
    }

    public static List<PhotoDetails> getPhotoDetailsById(String id) {
        try {
            return search(
                new TermQuery(new Term(PhotoDetails.PRIMARY_ID_KEY, id)),
                PHOTO_DETAIL_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search getPhotoDetailsById", e);
            return Collections.emptyList();
        }
    }

    public static List<List<PhotoDetails>> getMultiplePhotoDetailsById(List<String> ids) {
        try {
            List<List<PhotoDetails>> results = Collections.emptyList();
            for (var id : ids) {
                results.add(getPhotoDetailsById(id));
            }
            return results;
        } catch (Exception e) {
            logger.error("Could not search getMultiplePhotoDetailsById", e);
            return Collections.emptyList();
        }
    }

    private static <T> List<T> search(
        Query query,
        String indexPath,
        TypeReference<T> tr
    ) throws IOException {
        return search(query, indexPath, tr, 10);
    }


    private static <T> List<T> search(
        Query query,
        String indexPath,
        TypeReference<T> tr,
        int numResults
    ) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, numResults);

        List<T> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            var document = searcher.doc(scoreDoc.doc);
            for (var field : document.getFields(SOURCE_KEY)) {
                T result = objectMapper.readValue(
                    field.stringValue(),
                    tr);
                searchResults.add(result);
            }
        }
        return searchResults;
    }


    public static List<Species> getAllSpeciesByFirstLetter(
        String letter
    ) throws IOException {
        var MAX_RESULTS = 5000;

        Query query = new TermQuery(new Term(LETTER_KEY, letter));
        Sort sort = new Sort(new SortField(Species.NAME_KEY_FOR_SORT, SortField.Type.STRING));

        Directory indexDirectory = FSDirectory.open(Paths.get(SPECIES_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, MAX_RESULTS, sort);

        List<Species> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            var document = searcher.doc(scoreDoc.doc);
            for (var field : document.getFields(SOURCE_KEY)) {
                Species result = objectMapper.readValue(
                    field.stringValue(),
                    new TypeReference<Species>() {
                    }
                );
                searchResults.add(result);
            }
        }
        return searchResults;
    }

    public static List<Hybrid> getAllHybridsByFirstLetter(
        String letter
    ) throws IOException {
        var MAX_RESULTS = 5000;

        Query query = new TermQuery(new Term(LETTER_KEY, letter));
        Sort sort = new Sort(new SortField(Hybrid.NAME_KEY_FOR_SORT, SortField.Type.STRING));

        Directory indexDirectory = FSDirectory.open(Paths.get(HYBRIDS_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, MAX_RESULTS, sort);

        List<Hybrid> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            var document = searcher.doc(scoreDoc.doc);
            for (var field : document.getFields(SOURCE_KEY)) {
                Hybrid result = objectMapper.readValue(
                    field.stringValue(),
                    new TypeReference<Hybrid>() {
                    }
                );
                searchResults.add(result);
            }
        }
        return searchResults;
    }
}
