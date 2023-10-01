package com.rhododendra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.PrimaryID;
import com.rhododendra.model.Species;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiConsumer;


public class IndexService {
    public final static String BASE_INDEX_PATH = "/Users/john.verwolf/code/rhododendra/src/main/resources/index";
    public final static String BOTANIST_INDEX_PATH = BASE_INDEX_PATH + "/botanists";
    public final static String SPECIES_INDEX_PATH = BASE_INDEX_PATH + "/species";
    public final static String PHOTO_DETAIL_INDEX_PATH = BASE_INDEX_PATH + "/photo_details";


    public final static String SOURCE_KEY = "_source";

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static void indexSpecies() throws IOException {
        index(
            JSONLoaderService.loadSpecies(),
            SPECIES_INDEX_PATH,
            Species.PRIMARY_ID_KEY,
            (document, species) -> {
                document.add(new TextField(Species.NAME_KEY, species.getName(), Field.Store.NO));
            }
        );
    }

    public static void indexPhotoDetails() throws IOException {
        index(
            JSONLoaderService.loadPhotoDetails(),
            PHOTO_DETAIL_INDEX_PATH,
            PhotoDetails.PRIMARY_ID_KEY,
            (document, photoDetails) -> {
                document.add(new TextField(PhotoDetails.PHOTO_BY, photoDetails.getPhotoBy(), Field.Store.NO));
            }
        );
    }


    public static void indexBotanists() throws IOException {
        index(
            JSONLoaderService.loadBotanists(),
            BOTANIST_INDEX_PATH,
            Botanist.PRIMARY_ID_KEY,
            (document, botanist) -> {
                document.add(new TextField(Botanist.FULL_NAME_KEY, botanist.getFullName(), Field.Store.NO));
            }
        );
    }


    private static <T extends PrimaryID> void index(
        List<T> searchDocs,
        String indexPath,
        String primaryIdKey,
        BiConsumer<Document, T> searchableFields
    ) throws IOException {
        Directory indexDirectory = FSDirectory.open(
            Paths.get(indexPath)
        );
        // OPEN mode overwrites the existing index.
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

        for (T doc : searchDocs) {
            Document document = new Document();
            document.add(new Field(SOURCE_KEY, objectMapper.writeValueAsString(doc), sourceFieldType()));
            document.add(new StringField(primaryIdKey, doc.primaryIdValue(), Field.Store.YES));
            searchableFields.accept(document, doc);
            indexWriter.updateDocument(new Term(primaryIdKey, doc.primaryIdValue()), document);
        }
        indexWriter.close();
    }

    private static FieldType sourceFieldType() {
        var sourceFieldType = new FieldType();
        sourceFieldType.setStored(true);
        sourceFieldType.setTokenized(false);
        sourceFieldType.setOmitNorms(true);
        sourceFieldType.freeze();
        return sourceFieldType;
    }
}
