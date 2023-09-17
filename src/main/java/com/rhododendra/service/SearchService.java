package com.rhododendra.service;

import com.rhododendra.model.Botanist;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.rhododendra.service.IndexService.BOTANIST_INDEX_PATH;
import static com.rhododendra.service.IndexService.SOURCE_KEY;

public class SearchService {

    public static List<Document> searchBotanists(String inField, String queryString) throws IOException, ParseException {
        Query query = new QueryParser(inField, new StandardAnalyzer()).parse(queryString);
        Directory indexDirectory = FSDirectory.open(Paths.get(BOTANIST_INDEX_PATH));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);

        List<Botanist> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {

            var test = searcher.doc(scoreDoc.doc);
            for (var field : test.getFields(SOURCE_KEY)){
                field.stringValue();
            }

            searchResults.add();
        }

        return searchResults;
    }
}
