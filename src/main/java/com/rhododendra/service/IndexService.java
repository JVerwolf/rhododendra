package com.rhododendra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.db.BotanistRepository;
import com.rhododendra.db.HybridizerRepository;
import com.rhododendra.db.PhotoDetailsRepository;
import com.rhododendra.db.RhododendronRepository;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.Indexable;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.util.Util;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IndexService {
    public static final String BASE_INDEX_PATH = "index";
    public static final String BOTANIST_INDEX_PATH = BASE_INDEX_PATH + "/botanists";
    public static final String PHOTO_DETAIL_INDEX_PATH = BASE_INDEX_PATH + "/photo_details";
    public static final String RHODO_INDEX_PATH = BASE_INDEX_PATH + "/rhodos";
    public static final String HYBRIDIZER_INDEX_PATH = BASE_INDEX_PATH + "/hybridizers";


    //Additional Search/Index keys
    public static final String LETTER_KEY = "letter";
    public static final String HAS_PHOTOS = "has_photos";
    public static final String SEED_PARENT_KEY = "seed_parent";
    public static final String POLLEN_PARENT_KEY = "pollen_parent";
    public static final String SUBGENUS_KEY = "subgenus";
    public static final String SECTION_KEY = "section";
    public static final String SUBSECTION_KEY = "subsection";
    public static final String HYBRIDIZER_ID = "hybridizer_id";

    public static final String SOURCE_KEY = "_source";
    public static final String PAGINATION_DESCRIPTOR_KEY = "descriptor"; // for pagination

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static RhododendronRepository rhododendronRepository;
    private static HybridizerRepository hybridizerRepository;
    private static BotanistRepository botanistRepository;
    private static PhotoDetailsRepository photoDetailsRepository;
    private static com.rhododendra.db.Db db;

    public IndexService(
        RhododendronRepository rhododendronRepository,
        HybridizerRepository hybridizerRepository,
        BotanistRepository botanistRepository,
        PhotoDetailsRepository photoDetailsRepository,
        com.rhododendra.db.Db db
    ) {
        IndexService.rhododendronRepository = rhododendronRepository;
        IndexService.hybridizerRepository = hybridizerRepository;
        IndexService.botanistRepository = botanistRepository;
        IndexService.photoDetailsRepository = photoDetailsRepository;
        IndexService.db = db;
    }

    public static String generateRhodoNameForIndexing(Rhododendron rhodo, Map<String, Rhododendron> idToRhodoMap) {
        if (rhodo.getIs_species_selection()) {
            try {
                return idToRhodoMap.get(rhodo.getSpecies_id()).getName().toLowerCase() + " " + rhodo.getName().toLowerCase();
            } catch (Exception e) {
                System.out.println(e);
                return rhodo.getName().toLowerCase();
            }
        } else {
            return rhodo.getName().toLowerCase();
        }
    }


    public static void indexRhodos() throws IOException {
        List<Rhododendron> rhodos = loadAllRhodosFromDb();
        final var idToRhodoMap = rhodos.stream()
            .collect(Collectors.toMap(Rhododendron::getId, Function.identity()));

        index(
            rhodos,
            RHODO_INDEX_PATH,
            Rhododendron.PRIMARY_ID_KEY,
            (document, rhodo) -> {
                // TODO combine PAGINATION_DESCRIPTOR_KEY with the name key below?
                //  - have paginator refer to the name key to just store once?
                //  - likewise for NAME_KEY_FOR_SORT
                var nameforIndexing = generateRhodoNameForIndexing(rhodo, idToRhodoMap);
                document.add(new StringField(PAGINATION_DESCRIPTOR_KEY, nameforIndexing, Field.Store.YES));
                document.add(new TextField(Rhododendron.NAME_KEY, nameforIndexing, Field.Store.NO));
                document.add(new StringField(LETTER_KEY, Util.getfirstLetterForIndexing(nameforIndexing), Field.Store.NO));
                document.add(new SortedDocValuesField(Rhododendron.NAME_KEY_FOR_SORT, new BytesRef(nameforIndexing)));
                document.add(new StringField(Rhododendron.SEARCH_FILTERS, rhodo.getSearchFilter().name(), Field.Store.NO));
                document.add(new StringField(
                    HAS_PHOTOS,
                    rhodo.getPhotos().isEmpty() ? "false" : "true",
                    Field.Store.NO
                ));
                if (rhodo.getHybridizer() != null && rhodo.getHybridizer().getHybridizer_id() != null){
                    document.add(new StringField(HYBRIDIZER_ID,rhodo.getHybridizer().getHybridizer_id(), Field.Store.NO));
                }

                var parentage = rhodo.getParentage();
                if (parentage != null) {
                    var seed_parent = parentage.getSeed_parent_id();
                    if (seed_parent != null) {
                        document.add(new StringField(SEED_PARENT_KEY, seed_parent, Field.Store.NO));
                    }
                    var pollen_parent = parentage.getPollen_parent_id();
                    if (pollen_parent != null) {
                        document.add(new StringField(POLLEN_PARENT_KEY, pollen_parent, Field.Store.NO));
                    }
                } else if (rhodo.isSpecies()) {
                    document.add(new StringField(SEED_PARENT_KEY, rhodo.getId(), Field.Store.NO));
                    document.add(new StringField(POLLEN_PARENT_KEY, rhodo.getId(), Field.Store.NO));
                } else if (rhodo.getIs_species_selection()) {
                    document.add(new StringField(SEED_PARENT_KEY, rhodo.getSpecies_id(), Field.Store.NO));
                    document.add(new StringField(POLLEN_PARENT_KEY, rhodo.getSpecies_id(), Field.Store.NO));
                }

                // Set the taxonomy of a species selection to the original species
                if (rhodo.getIs_species_selection()) {
                    var originalSpecies = idToRhodoMap.get(rhodo.getSpecies_id());
                    rhodo.setTaxonomy(originalSpecies.getTaxonomy());
                }

                if (rhodo.getTaxonomy() != null) {
                    if (rhodo.getTaxonomy().getSubgenus() != null) {
                        document.add(new StringField(SUBGENUS_KEY, rhodo.getTaxonomy().getSubgenus().toLowerCase(), Field.Store.NO));
                    }
                    if (rhodo.getTaxonomy().getSection() != null) {
                        document.add(new StringField(SECTION_KEY, rhodo.getTaxonomy().getSection().toLowerCase(), Field.Store.NO));
                    }
                    if (rhodo.getTaxonomy().getSubsection() != null) {
                        document.add(new StringField(SUBSECTION_KEY, rhodo.getTaxonomy().getSubsection().toLowerCase(), Field.Store.NO));
                    }
                }
            }
        );
    }

    public static void indexHybridizers() throws IOException {
        List<Hybridizer> hybridizers = loadAllHybridizersFromDb();
        index(
            hybridizers,
            HYBRIDIZER_INDEX_PATH,
            Hybridizer.PRIMARY_ID_KEY,
            (document, hybridizer) -> {
                document.add(new SortedDocValuesField(Hybridizer.NAME_KEY_FOR_SORT, new BytesRef(hybridizer.getName())));
                document.add(new TextField(Hybridizer.NAME_KEY, hybridizer.getName(), Field.Store.NO));
                document.add(new StringField(PAGINATION_DESCRIPTOR_KEY, hybridizer.getName(), Field.Store.YES));
            }
        );
    }

    public static void indexPhotoDetails() throws IOException {
        List<PhotoDetails> photos = loadAllPhotoDetailsFromDb();
        index(
            photos,
            PHOTO_DETAIL_INDEX_PATH,
            PhotoDetails.PRIMARY_ID_KEY,
            (document, photoDetails) -> {
                if (photoDetails.getPhotoBy() != null) {
                    document.add(new TextField(PhotoDetails.PHOTO_BY, photoDetails.getPhotoBy(), Field.Store.NO));
                }
            }
        );
    }


    public static void indexBotanists() throws IOException {
        List<Botanist> botanists = loadAllBotanistsFromDb();
        index(
            botanists,
            BOTANIST_INDEX_PATH,
            Botanist.PRIMARY_ID_KEY,
            (document, botanist) -> {
                document.add(new TextField(Botanist.FULL_NAME_KEY, botanist.getFullName(), Field.Store.NO));
            }
        );
    }


    private static <T extends Indexable> void index(
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
            searchableFields.accept(document, doc);
            document.add(new Field(SOURCE_KEY, objectMapper.writeValueAsString(doc), sourceFieldType()));
            document.add(new StringField(primaryIdKey, doc.primaryIdValue(), Field.Store.YES));
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

    private static FieldType hasPhotosFieldType() {
        var sourceFieldType = new FieldType();
        sourceFieldType.setStored(true);
        sourceFieldType.setTokenized(false);
        sourceFieldType.setOmitNorms(true);
        sourceFieldType.freeze();
        return sourceFieldType;
    }
    private static List<Rhododendron> loadAllRhodosFromDb() {
        List<Rhododendron> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT id FROM rhododendron");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                Rhododendron rhodo = rhododendronRepository.getById(id);
                if (rhodo != null) {
                    result.add(rhodo);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load rhododendrons from database", e);
        }
        return result;
    }

    private static List<Hybridizer> loadAllHybridizersFromDb() {
        List<Hybridizer> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT id FROM hybridizer");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                Hybridizer h = hybridizerRepository.getById(id);
                if (h != null) {
                    result.add(h);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load hybridizers from database", e);
        }
        return result;
    }

    private static List<Botanist> loadAllBotanistsFromDb() {
        List<Botanist> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT botanical_short FROM botanist");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("botanical_short");
                Botanist b = botanistRepository.getById(id);
                if (b != null) {
                    result.add(b);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load botanists from database", e);
        }
        return result;
    }

    private static List<PhotoDetails> loadAllPhotoDetailsFromDb() {
        List<PhotoDetails> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT photo FROM photo_details");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("photo");
                PhotoDetails p = photoDetailsRepository.getById(id);
                if (p != null) {
                    result.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load photo details from database", e);
        }
        return result;
    }
}

