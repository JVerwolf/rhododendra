/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.rhododendra.service;

import com.rhododendra.db.BotanistRepository;
import com.rhododendra.db.HybridizerRepository;
import com.rhododendra.db.PhotoDetailsRepository;
import com.rhododendra.db.RhododendronRepository;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rhododendra.model.Rhododendron.PRIMARY_ID_KEY;
import static com.rhododendra.model.Rhododendron.SEARCH_FILTERS;
import static com.rhododendra.service.IndexService.*;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private static RhododendronRepository rhododendronRepository;
    private static HybridizerRepository hybridizerRepository;
    private static BotanistRepository botanistRepository;
    private static PhotoDetailsRepository photoDetailsRepository;

    public SearchService(
        RhododendronRepository rhododendronRepository,
        HybridizerRepository hybridizerRepository,
        BotanistRepository botanistRepository,
        PhotoDetailsRepository photoDetailsRepository,
        IndexService indexService
    ) {
        Objects.requireNonNull(
            indexService,
            "IndexService must be constructed first so Lucene index paths are initialized"
        );
        SearchService.rhododendronRepository = rhododendronRepository;
        SearchService.hybridizerRepository = hybridizerRepository;
        SearchService.botanistRepository = botanistRepository;
        SearchService.photoDetailsRepository = photoDetailsRepository;
    }

    public static List<Botanist> searchBotanists(String queryString) {
        try {
            var query = new QueryParser(Botanist.FULL_NAME_KEY, new StandardAnalyzer()).parse(queryString);
            Directory indexDirectory = FSDirectory.open(Paths.get(IndexService.botanistIndexPath()));
            IndexReader indexReader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 10);

            List<Botanist> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                var document = searcher.doc(scoreDoc.doc);
                var id = document.get(Botanist.PRIMARY_ID_KEY);
                if (id != null) {
                    Botanist botanist = botanistRepository.getById(Long.parseLong(id));
                    if (botanist != null) {
                        results.add(botanist);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.error("Could not search searchBotanists", e);
            return Collections.emptyList();
        }
    }

    public static IndexResults<Rhododendron> searchByParentage(
        Long seedParentId,
        Long pollenParentId,
        boolean requireSeed,
        boolean requirePollen,
        boolean allowReverse,
        Long originalRhodoId,
        int pageSize,
        int offset
    ) throws IOException {

        var geneticQuery = new BooleanQuery.Builder();
        if (seedParentId != null) {
            var seedParentIdStr = String.valueOf(seedParentId);
            var seedQuery = new BooleanQuery.Builder();
            seedQuery.add(new TermQuery(new Term(SEED_PARENT_KEY, seedParentIdStr)), BooleanClause.Occur.SHOULD);
            if (allowReverse) {
                seedQuery.add(new TermQuery(new Term(POLLEN_PARENT_KEY, seedParentIdStr)), BooleanClause.Occur.SHOULD);
            }
            geneticQuery.add(
                seedQuery.build(),
                requireSeed ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD
            );
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, seedParentIdStr)), BooleanClause.Occur.MUST_NOT);
        }
        if (pollenParentId != null) {
            var pollenParentIdStr = String.valueOf(pollenParentId);
            var pollenQuery = new BooleanQuery.Builder();
            pollenQuery.add(new TermQuery(new Term(POLLEN_PARENT_KEY, pollenParentIdStr)), BooleanClause.Occur.SHOULD);
            if (allowReverse) {
                pollenQuery.add(new TermQuery(new Term(SEED_PARENT_KEY, pollenParentIdStr)), BooleanClause.Occur.SHOULD);
            }
            geneticQuery.add(
                pollenQuery.build(),
                requirePollen ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD
            );
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, pollenParentIdStr)), BooleanClause.Occur.MUST_NOT);
        }
        if (originalRhodoId != null) {
            geneticQuery.add(new TermQuery(new Term(PRIMARY_ID_KEY, String.valueOf(originalRhodoId))), BooleanClause.Occur.MUST_NOT);
        }


        return paginatedRhodoSearch(
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

        return paginatedRhodoSearch(
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(query, Integer.MAX_VALUE)
        );

    }

    public static IndexResults<Rhododendron> getRhodosByHybridizer(Long hybridizerId, int pageSize, int offset) throws IOException, ParseException {
        Query query;
        if (hybridizerId == null) {
            query = new BooleanQuery.Builder().build(); // matches nothing if query is blank
        } else {
            query = new TermQuery(new Term(HYBRIDIZER_ID, String.valueOf(hybridizerId)));
        }
        // TODO add a sort by date
        Sort sort = new Sort(new SortField(Rhododendron.NAME_KEY_FOR_SORT, SortField.Type.STRING));
        return paginatedRhodoSearch(
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(query, Integer.MAX_VALUE, sort)
        );

    }

    public static IndexResults<Hybridizer> searchHybridizers(String queryString, int pageSize, int offset) throws IOException, ParseException {
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
                        queryBuilder.createMinShouldMatchQuery(Hybridizer.NAME_KEY, queryString, .75f),
                        BooleanClause.Occur.SHOULD
                    )
                )
                .add(new BooleanClause(
                    new FuzzyQuery(new Term(Hybridizer.NAME_KEY, queryString), maxEdits),
                    BooleanClause.Occur.SHOULD)
                )
                .build();
        }

        return paginatedHybridizerSearch(
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(query, Integer.MAX_VALUE)
        );

    }

    public static IndexResults<Rhododendron> searchRhodosByTaxonomy(String subgenus, String section, String subsection, int pageSize, int offset) throws IOException, ParseException {
        var query = new BooleanQuery.Builder();
        if (!Strings.isEmpty(subgenus)) {
            query.add(new TermQuery(new Term(SUBGENUS_KEY, subgenus.toLowerCase())), BooleanClause.Occur.MUST);
        } else if (!Strings.isEmpty(section)) {
            query.add(new TermQuery(new Term(SECTION_KEY, section.toLowerCase())), BooleanClause.Occur.MUST);
        } else if (!Strings.isEmpty(subsection)) {
            query.add(new TermQuery(new Term(SUBSECTION_KEY, subsection.toLowerCase())), BooleanClause.Occur.MUST);
        }
        Sort sort = new Sort(new SortField(Rhododendron.NAME_KEY_FOR_SORT, SortField.Type.STRING));
        return paginatedRhodoSearch(
            pageSize,
            offset,
            indexSearcher -> indexSearcher.search(query.build(), Integer.MAX_VALUE, sort)
        );
    }

    public static List<PhotoDetails> searchPhotoDetails(String queryString) {
        try {
            var query = new QueryParser(PhotoDetails.PHOTO_BY, new StandardAnalyzer()).parse(queryString);
            Directory indexDirectory = FSDirectory.open(Paths.get(IndexService.photoDetailIndexPath()));
            IndexReader indexReader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 10);

            List<PhotoDetails> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                var document = searcher.doc(scoreDoc.doc);
                var id = document.get(PhotoDetails.PRIMARY_ID_KEY);
                if (id != null) {
                    PhotoDetails p = photoDetailsRepository.getById(Long.parseLong(id));
                    if (p != null) {
                        results.add(p);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.error("Could not search searchPhotoDetails", e);
            return Collections.emptyList();
        }
    }

    public static List<String> getAllRhodoIds() {
        Query query = new MatchAllDocsQuery();
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get(IndexService.rhodoIndexPath()));
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

    public static List<Botanist> getBotanistById(Long id) {
        if (id == null) return List.of();
        try {
            Botanist botanist = botanistRepository.getById(id);
            if (botanist == null) {
                return List.of();
            }
            return List.of(botanist);
        } catch (Exception e) {
            logger.error("Could not load Botanist from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<Botanist> getBotanistByBotanicalShort(String botanicalShort) {
        if (botanicalShort == null) return List.of();
        try {
            Botanist botanist = botanistRepository.getByBotanicalShort(botanicalShort);
            if (botanist == null) {
                return List.of();
            }
            return List.of(botanist);
        } catch (Exception e) {
            logger.error("Could not load Botanist by botanical short from DB", e);
            return Collections.emptyList();
        }
    }


    public static List<Rhododendron> getRhodoById(Long id) {
        if (id == null) return List.of();
        try {
            Rhododendron rhodo = rhododendronRepository.getById(id);
            if (rhodo == null) {
                return List.of();
            }
            return List.of(rhodo);
        } catch (Exception e) {
            logger.error("Could not load Rhododendron from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<Rhododendron> getRhodoByOldId(String oldId) {
        if (oldId == null) return List.of();
        try {
            Rhododendron rhodo = rhododendronRepository.getByOldId(oldId);
            if (rhodo == null) {
                return List.of();
            }
            return List.of(rhodo);
        } catch (Exception e) {
            logger.error("Could not load Rhododendron by old id from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<Hybridizer> getHybridizerById(Long id) {
        if (id == null) return List.of();
        try {
            Hybridizer h = hybridizerRepository.getById(id);
            if (h == null) {
                return List.of();
            }
            return List.of(h);
        } catch (Exception e) {
            logger.error("Could not load Hybridizer from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<Hybridizer> getHybridizerByOldId(String oldId) {
        if (oldId == null) return List.of();
        try {
            Hybridizer h = hybridizerRepository.getByOldId(oldId);
            if (h == null) {
                return List.of();
            }
            return List.of(h);
        } catch (Exception e) {
            logger.error("Could not load Hybridizer by old id from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<PhotoDetails> getPhotoDetailsById(Long id) {
        if (id == null) return List.of();
        try {
            PhotoDetails p = photoDetailsRepository.getById(id);
            if (p == null) {
                return List.of();
            }
            return List.of(p);
        } catch (Exception e) {
            logger.error("Could not load PhotoDetails from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<PhotoDetails> getPhotoDetailsByPhoto(String photo) {
        if (photo == null) return List.of();
        try {
            PhotoDetails p = photoDetailsRepository.getByPhoto(photo);
            if (p == null) {
                return List.of();
            }
            return List.of(p);
        } catch (Exception e) {
            logger.error("Could not load PhotoDetails by photo filename from DB", e);
            return Collections.emptyList();
        }
    }

    public static List<PhotoDetails> getMultiplePhotoDetailsById(List<String> ids) {
        try {
            return ids.stream()
                .map(SearchService::getPhotoDetailsByPhoto)
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
        return paginatedRhodoSearch(
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

    private static IndexResults<Rhododendron> paginatedRhodoSearch(
        int pageSize,
        int offset,
        CheckedFunction<IndexSearcher, TopDocs, IOException> performSearch
    ) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(IndexService.rhodoIndexPath()));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = performSearch.apply(searcher);

        // Read the Source values via DB hydration (inclusive range: pageSize items starting at offset).
        int len = topDocs.scoreDocs.length;
        int startPos = offset;
        int endPos = Math.min(len - 1, offset + pageSize - 1);
        List<Rhododendron> results = new ArrayList<>();
        if (len > 0 && startPos < len && startPos <= endPos) {
        for (int i = startPos; i <= endPos; i++) {
            var doc = searcher.doc(topDocs.scoreDocs[i].doc);
            var id = doc.get(PRIMARY_ID_KEY);
            if (id != null) {
                try {
                    Rhododendron rhodo = rhododendronRepository.getById(Long.parseLong(id));
                    if (rhodo != null) {
                        results.add(rhodo);
                    }
                } catch (SQLException e) {
                    throw new IOException("Failed to load rhododendron from DB for id " + id, e);
                }
            }
        }
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

        var indexPagePosition = getPageIndex(offset, pageSize, len);

        return new IndexResults<>(
            indexPages,
            indexPagePosition,
            results
        );
    }

    private static IndexResults<Hybridizer> paginatedHybridizerSearch(
        int pageSize,
        int offset,
        CheckedFunction<IndexSearcher, TopDocs, IOException> performSearch
    ) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(IndexService.hybridizerIndexPath()));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = performSearch.apply(searcher);

        int hLen = topDocs.scoreDocs.length;
        int hStart = offset;
        int hEnd = Math.min(hLen - 1, offset + pageSize - 1);
        List<Hybridizer> results = new ArrayList<>();
        if (hLen > 0 && hStart < hLen && hStart <= hEnd) {
        for (int i = hStart; i <= hEnd; i++) {
            var doc = searcher.doc(topDocs.scoreDocs[i].doc);
            var id = doc.get(Hybridizer.PRIMARY_ID_KEY);
            if (id != null) {
                try {
                    Hybridizer h = hybridizerRepository.getById(Long.parseLong(id));
                    if (h != null) {
                        results.add(h);
                    }
                } catch (SQLException e) {
                    throw new IOException("Failed to load hybridizer from DB for id " + id, e);
                }
            }
        }
        }

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

        var indexPagePosition = getPageIndex(offset, pageSize, hLen);

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
        var numPages = (listLength + pageSize - 1) / pageSize;
        List<T> results = new ArrayList<>();
        for (int pageNum = 0; pageNum < numPages; pageNum++) {
            int pageStart = Math.min(pageNum * pageSize, listLength - 1);
            int pageEnd = Math.min(pageStart + pageSize - 1, listLength - 1);
            results.add(fun.apply(pageStart, pageEnd));
        }
        return results;
    }
}

