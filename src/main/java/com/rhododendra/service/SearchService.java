package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.*;
import com.rhododendra.util.CheckedBiFunction;
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

    public static List<Rhododendron> searchRhodos(String queryString) {
        try {
            return search(
                new QueryParser(Rhododendron.NAME_KEY, new StandardAnalyzer()).parse(queryString),
                RHODO_INDEX_PATH,
                new TypeReference<>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search searchRhodos", e);
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


    public static List<Rhododendron> getRhodoById(String id) {
        try {
            return search(
                new TermQuery(new Term(Rhododendron.PRIMARY_ID_KEY, id)),
                RHODO_INDEX_PATH,
                new TypeReference<Rhododendron>() {
                }
            );
        } catch (Exception e) {
            logger.error("Could not search getRhodoById", e);
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

    public static List<PhotoDetails> getMultiplePhotoDetailsById(List<String> ids) {
        try {
            return ids.stream()
                .map(SearchService::getPhotoDetailsById)
                .filter(photoDetails -> photoDetails != null && !photoDetails.isEmpty())
                .map((photoDetails) -> photoDetails.get(0))
                .toList();
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


    public static class IndexPage {
        public String startValue;
        public int startPos;
        public String endValue;
        public int endPos;

        public IndexPage(String startValue, int startPos, String endValue, int endPos) {
            this.startValue = startValue;
            this.startPos = startPos;
            this.endValue = endValue;
            this.endPos = endPos;
        }
    }


    public static class RhodoIndexResults {
        public List<IndexPage> indexPages;
        public int indexPagePos;
        public List<Rhododendron> rhodos;

        public RhodoIndexResults(List<IndexPage> indexPages, int indexPagePos, List<Rhododendron> rhodos) {
            this.indexPages = indexPages;
            this.indexPagePos = indexPagePos;
            this.rhodos = rhodos;
        }
    }

    private static String readPaginationDescriptor(
        ScoreDoc scoreDoc,
        IndexSearcher searcher
    ) throws IOException {
        var document = searcher.doc(scoreDoc.doc);
        var field = document.getField(PAGINATION_DESCRIPTOR_KEY);
        return field.stringValue();
    }


    private static Rhododendron readRhodoSource(
        ScoreDoc scoreDoc,
        IndexSearcher searcher
    ) throws IOException {
        var document = searcher.doc(scoreDoc.doc);
        var field = document.getField(SOURCE_KEY);
        return objectMapper.readValue(
            field.stringValue(),
            new TypeReference<Rhododendron>() {
            }
        );
    }


    public static RhodoIndexResults getAllRhodosByFirstLetter(
        String letter,
        int pageSize,
        int offset
    ) throws IOException {
        var MAX_RESULTS = 5000;

        Query query = new TermQuery(new Term(LETTER_KEY, letter));
        Sort sort = new Sort(new SortField(Rhododendron.NAME_KEY_FOR_SORT, SortField.Type.STRING));

        Directory indexDirectory = FSDirectory.open(Paths.get(RHODO_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, MAX_RESULTS, sort);

        // Read the Source values.
        var startPos = Math.min(topDocs.scoreDocs.length, offset); // todo off by 1?
        var endPos = Math.min(topDocs.scoreDocs.length - 1, offset + pageSize); // todo off by 1?
        List<Rhododendron> rhododendrons = new ArrayList<>();
        for (int i = startPos; i <= endPos; i++) {
            var rhodo = readRhodoSource(topDocs.scoreDocs[i], searcher);
            rhododendrons.add(rhodo);
        }

        // Read the index pages.
        var indexPages = paginationOffsets(
            pageSize,
            topDocs.scoreDocs.length,
            (pageStart, pageEnd) -> {
                var startValue = readPaginationDescriptor(topDocs.scoreDocs[pageStart], searcher);
                var endValue = readPaginationDescriptor(topDocs.scoreDocs[pageEnd], searcher);
                return new IndexPage(
                    startValue.substring(0, Math.min(3, startValue.length())),
                    pageStart,
                    endValue.substring(0, Math.min(3, endValue.length())),
                    pageEnd
                );
            }
        );

        var pageNum = offset / pageSize;
        var indexPagePosition = Math.min(indexPages.size(), pageNum);

        return new RhodoIndexResults(
            indexPages,
            indexPagePosition,
            rhododendrons
        );
    }


    public static int getPageIndex(int offset, int pageSize, int resultsLen) {
        var lastPageIndex = Math.max((resultsLen - 1) / pageSize, 0);
        var pageIndex = offset / pageSize;
        return Math.min(pageIndex, lastPageIndex);
    }


    public static <T> List<T> paginationOffsets(int pageSize, int listLength, CheckedBiFunction<Integer, Integer, T, IOException> fun) throws IOException {
        var numPages = (listLength / pageSize) + 1;
        List<T> results = new ArrayList<>();
        for (int pageNum = 0; pageNum < numPages; pageNum++) {
            int pageStart = pageNum * pageSize;
            int pageEnd = Math.min(pageStart + pageSize - 1, listLength - 1);
            results.add(fun.apply(pageStart, pageEnd));
        }
        return results;
    }
}

