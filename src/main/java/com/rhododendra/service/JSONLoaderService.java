package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JSONLoaderService {
    final static String BOTANISTS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species_botanists.json";
    final static String SPECIES_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species.json";
    final static String HYBRIDS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/hybrids.json";
    final static String AZALEAS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/azaleas.json";
    final static String VIREYAS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/vireyas.json";
    final static String HYBRIDIZER_DETAILS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/hybridizers.json";
    final static String AZALEODENDRONS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/azaleodendrons.json";
    final static String PHOTO_DETAILS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/photo_details.json";

    public static List<Botanist> loadBotanists() throws IOException {
        File file = new File(BOTANISTS_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }

    public static List<Rhododendron> loadRhodos() throws IOException {
        File speciesFile = new File(SPECIES_PATH);
        File hybridsFile = new File(HYBRIDS_PATH);
        File azaleasFile = new File(AZALEAS_PATH);
        File vireyasFile = new File(VIREYAS_PATH);
        File azaleodendronsFile = new File(AZALEODENDRONS_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Rhododendron> result = objectMapper.readValue(speciesFile, new TypeReference<List<Rhododendron>>() {
        });
        result.addAll(objectMapper.readValue(hybridsFile, new TypeReference<List<Rhododendron>>() {
        }));
        result.addAll(objectMapper.readValue(azaleasFile, new TypeReference<List<Rhododendron>>() {
        }));
        result.addAll(objectMapper.readValue(vireyasFile, new TypeReference<List<Rhododendron>>() {
        }));
        result.addAll(objectMapper.readValue(azaleodendronsFile, new TypeReference<List<Rhododendron>>() {
        }));
        return result;
    }

    public static List<PhotoDetails> loadPhotoDetails() throws IOException {
        File file = new File(PHOTO_DETAILS_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }

    public static List<Hybridizer> loadHybridizers() throws IOException {
        File file = new File(HYBRIDIZER_DETAILS_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }
}
