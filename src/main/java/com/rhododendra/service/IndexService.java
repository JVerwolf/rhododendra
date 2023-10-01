package com.rhododendra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Species;
import org.apache.lucene.document.*;
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
    public final static String SPECIES_INDEX_PATH = BASE_INDEX_PATH + "/species";

    public final static String SOURCE_KEY = "_source";

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

        for (Botanist botanist : JSONLoaderService.loadBotanists()) {
            String source = objectMapper.writeValueAsString(botanist);

            Document document = new Document();
            document.add(new Field(SOURCE_KEY, source, souceFieldType()));
            document.add(new TextField(Botanist.FULL_NAME_KEY, botanist.getFullName(), Field.Store.NO));
            document.add(new StringField(Botanist.PRIMARY_ID_KEY, botanist.primaryIdValue(), Field.Store.YES));
            indexWriter.updateDocument(new Term(Botanist.PRIMARY_ID_KEY, botanist.primaryIdValue()), document);
        }
        indexWriter.close();
    }


    public static void indexSpecies() throws IOException {
        Directory indexDirectory = FSDirectory.open(
            Paths.get(SPECIES_INDEX_PATH)
        );
        // OPEN mode overwrites the existing index.
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

        for (Species species : JSONLoaderService.loadSpecies()) {
            String source = objectMapper.writeValueAsString(species);

            Document document = new Document();
            document.add(new Field(SOURCE_KEY, source, souceFieldType()));
            document.add(new TextField(Species.NAME_KEY, species.getName(), Field.Store.NO));
            document.add(new StringField(Species.PRIMARY_ID_KEY, species.primaryIdValue(), Field.Store.YES));

            indexWriter.updateDocument(new Term(Species.PRIMARY_ID_KEY, species.primaryIdValue()), document);
        }
        indexWriter.close();
    }
}
