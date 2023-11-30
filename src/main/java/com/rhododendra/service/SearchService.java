package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.model.Rhododendron.SearchFilters;
import com.rhododendra.util.CheckedBiFunction;
import com.rhododendra.util.CheckedFunction;
import org.apache.logging.log4j.util.Strings;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rhododendra.model.Rhododendron.PRIMARY_ID_KEY;
import static com.rhododendra.model.Rhododendron.SEARCH_FILTERS;
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

    public static IndexResults<Rhododendron> searchByParentage(
        String seedParentId,
        String pollenParentId,
        boolean requireSeed,
        boolean requirePollen,
        boolean allowReverse,
        String originalRhodoId,
        int pageSize,
        int offset
    ) throws IOException {

        var geneticQuery = new BooleanQuery.Builder();
        if (seedParentId != null) {
            var seedQuery = new BooleanQuery.Builder();
            seedQuery.add(new TermQuery(new Term(SEED_PARENT_KEY, seedParentId)), BooleanClause.Occur.SHOULD);
            if (allowReverse) {
                seedQuery.add(new TermQuery(new Term(POLLEN_PARENT_KEY, seedParentId)), BooleanClause.Occur.SHOULD);
            }
            geneticQuery.add(
                seedQuery.build(),
                requireSeed ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD
            );
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, seedParentId)), BooleanClause.Occur.MUST_NOT);
        }
        if (pollenParentId != null) {
            var pollenQuery = new BooleanQuery.Builder();
            pollenQuery.add(new TermQuery(new Term(POLLEN_PARENT_KEY, pollenParentId)), BooleanClause.Occur.SHOULD);
            if (allowReverse) {
                pollenQuery.add(new TermQuery(new Term(SEED_PARENT_KEY, pollenParentId)), BooleanClause.Occur.SHOULD);
            }
            geneticQuery.add(
                pollenQuery.build(),
                requirePollen ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD
            );
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, pollenParentId)), BooleanClause.Occur.MUST_NOT);
        }
        if (originalRhodoId != null) {
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, originalRhodoId)), BooleanClause.Occur.MUST_NOT);
        }


        return paginatedSearch(
            RHODO_INDEX_PATH,
            new TypeReference<Rhododendron>() {
            },
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(geneticQuery.build(), Integer.MAX_VALUE)
        );
    }

    public static IndexResults<Rhododendron> searchRhodos(String queryString, int pageSize, int offset) throws IOException, ParseException {
        Query query;

        if (Strings.isEmpty(queryString)) {
            query = new BooleanQuery.Builder().build(); // matches nothing if query is blank

        } else {
            int maxEdits = 0;
            if (queryString.length() <= 2) {
                maxEdits = 0;
            } else if (queryString.length() <= 5) {
                maxEdits = 2;
            } else {
                maxEdits = 2;
            }

            QueryBuilder queryBuilder = new QueryBuilder(new StandardAnalyzer());
            query = new BooleanQuery.Builder()
                .add(
                    new BooleanClause(
                        queryBuilder.createMinShouldMatchQuery(Rhododendron.NAME_KEY, queryString, .75f),
                        BooleanClause.Occur.SHOULD
                    )
                )
                .add(new BooleanClause(
                    new FuzzyQuery(new Term(Rhododendron.NAME_KEY, queryString), maxEdits),
                    BooleanClause.Occur.SHOULD)
                )
                .build();
        }

        return paginatedSearch(
            RHODO_INDEX_PATH,
            new TypeReference<Rhododendron>() {
            },
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(query, Integer.MAX_VALUE)
        );

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

    public static List<String> getAllRhodoIds() {
        Query query = new MatchAllDocsQuery();
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get(RHODO_INDEX_PATH));
            IndexReader indexReader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

            List<String> searchResults = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                var document = searcher.doc(scoreDoc.doc);
                for (var field : document.getFields(Rhododendron.PRIMARY_ID_KEY)) {
                    searchResults.add(field.stringValue());
                }
            }
            return searchResults;
        } catch (Exception e) {
            logger.error("Could not search getAllIds", e);
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
        if (id == null) return List.of();
        try {
            return search(
                new TermQuery(new Term(PRIMARY_ID_KEY, id)),
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

    public static IndexResults<Rhododendron> getAllRhodosByFirstLetter(
        String letter,
        int pageSize,
        int offset,
        boolean onlyPics, // TODO refactor to Settings Object
        List<SearchFilters> searchFilters // TODO refactor to Settings Object
    ) throws IOException {
        return paginatedSearch(
            RHODO_INDEX_PATH,
            new TypeReference<Rhododendron>() {
            },
            pageSize,
            offset,
            (searcher) -> {
                var query = new BooleanQuery.Builder()
                    .add(new BooleanClause(new TermQuery(new Term(LETTER_KEY, letter)), BooleanClause.Occur.MUST));
                if (onlyPics) {
                    query.add(new BooleanClause(new TermQuery(new Term(HAS_PHOTOS, "true")), BooleanClause.Occur.MUST));
                }
                if (searchFilters != null && !searchFilters.isEmpty()) {
                    var subQuery = new BooleanQuery.Builder();
                    for (var searchFilter : searchFilters) {
                        subQuery.add(new BooleanClause(new TermQuery(new Term(SEARCH_FILTERS, searchFilter.name())), BooleanClause.Occur.SHOULD));
                    }
                    query.add(subQuery.build(), BooleanClause.Occur.MUST);
                }
                Sort sort = new Sort(new SortField(Rhododendron.NAME_KEY_FOR_SORT, SortField.Type.STRING));
                return searcher.search(query.build(), Integer.MAX_VALUE, sort);
            }
        );
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
        public String letter;
        public List<Rhododendron> rhodos;

        public RhodoIndexResults(List<IndexPage> indexPages, int indexPagePos, String letter, List<Rhododendron> rhodos) {
            this.indexPages = indexPages;
            this.indexPagePos = indexPagePos;
            this.letter = letter;
            this.rhodos = rhodos;
        }
    }

    public static class IndexResults<T> {
        public List<IndexPage> indexPages;
        public int indexPagePos;
        public List<T> results;

        public IndexResults(List<IndexPage> indexPages, int indexPagePos, List<T> results) {
            this.indexPages = indexPages;
            this.indexPagePos = indexPagePos;
            this.results = results;
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

    private static <T> T readSource(
        ScoreDoc scoreDoc,
        IndexSearcher searcher,
        TypeReference<T> tr
    ) throws IOException {
        var document = searcher.doc(scoreDoc.doc);
        var field = document.getField(SOURCE_KEY);
        return objectMapper.readValue(
            field.stringValue(),
            tr
        );
    }

    private static <T> IndexResults<T> paginatedSearch(
        String indexPath,
        TypeReference<T> tr,
        int pageSize,
        int offset,
        CheckedFunction<IndexSearcher, TopDocs, IOException> performSearch
    ) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = performSearch.apply(searcher);

        // Read the Source values.
        var startPos = Math.min(topDocs.scoreDocs.length, offset); // todo off by 1?
        var endPos = Math.min(topDocs.scoreDocs.length - 1, offset + pageSize); // todo off by 1?
        List<T> results = new ArrayList<>();
        for (int i = startPos; i <= endPos; i++) {
            var result = readSource(topDocs.scoreDocs[i], searcher, tr);
            results.add(result);
        }

        // Read the index pages.
        var indexPages = paginationOffsets(
            pageSize,
            topDocs.scoreDocs.length,
            (pageStart, pageEnd) -> {
                var startValue = readPaginationDescriptor(topDocs.scoreDocs[pageStart], searcher);
                var endValue = readPaginationDescriptor(topDocs.scoreDocs[pageEnd], searcher);
                return new IndexPage(
                    startValue.substring(0, Math.min(4, startValue.length())),
                    pageStart,
                    endValue.substring(0, Math.min(4, endValue.length())),
                    pageEnd
                );
            }
        );

        var pageNum = offset / pageSize;
        var indexPagePosition = Math.min(indexPages.size(), pageNum);

        return new IndexResults<>(
            indexPages,
            indexPagePosition,
            results
        );
    }


    public static int getPageIndex(int offset, int pageSize, int resultsLen) {
        var lastPageIndex = Math.max((resultsLen - 1) / pageSize, 0);
        var pageIndex = offset / pageSize;
        return Math.min(pageIndex, lastPageIndex);
    }


    public static <T> List<T> paginationOffsets(int pageSize, int listLength, CheckedBiFunction<Integer, Integer, T, IOException> fun) throws IOException {
        if (listLength == 0) return List.of();
        var numPages = (listLength / pageSize) + 1;
        List<T> results = new ArrayList<>();
        for (int pageNum = 0; pageNum < numPages; pageNum++) {
            int pageStart = Math.min(pageNum * pageSize, listLength - 1);
            int pageEnd = Math.min(pageStart + pageSize - 1, listLength - 1);
            results.add(fun.apply(pageStart, pageEnd));
        }
        return results;
    }
}

