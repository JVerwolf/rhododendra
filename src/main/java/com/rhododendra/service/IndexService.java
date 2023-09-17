package com.rhododendra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;


public class IndexService {
    public final static String BASE_INDEX_PATH = "/Users/john.verwolf/code/rhododendra/src/main/resources/index";
    public final static String BOTANIST_INDEX_PATH = BASE_INDEX_PATH + "/botanists";

    public final static String SOURCE_KEY = "_source";
    public final static String BOTANIST_FULL_NAME_KEY = "full_name";
    public final static String BOTANIST_BOTANICAL_SHORT_KEY = "botanical_short";


    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static FieldType souceFieldType() {
        var sourceFieldType = new FieldType();
        sourceFieldType.setStored(true);
        sourceFieldType.setTokenized(false);
        sourceFieldType.setOmitNorms(true);
        sourceFieldType.freeze();
        return sourceFieldType;
    }


    public static void indexBotanists() throws IOException {
        Directory indexDirectory = FSDirectory.open(
            Paths.get(BOTANIST_INDEX_PATH)
        );
        // OPEN mode overwrites the existing index.
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

        for (Botanist botanist : JSONLoaderService.readBotanists()) {
            String source = objectMapper.writeValueAsString(botanist);

            Document document = new Document();
            document.add(new Field(SOURCE_KEY, source, souceFieldType()));
            document.add(new TextField(BOTANIST_FULL_NAME_KEY, botanist.getFullName(), Field.Store.NO));
            document.add(new TextField(BOTANIST_BOTANICAL_SHORT_KEY, botanist.getBotanicalShort(), Field.Store.NO));
            indexWriter.updateDocument(new Term(botanist.getPrimaryID()), document);
        }
        indexWriter.close();
    }

}
