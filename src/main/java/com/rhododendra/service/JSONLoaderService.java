package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Species;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JSONLoaderService {
    final static String BOTANISTS_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species_botanists.json";
    final static String SPECIES_PATH = "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species.json";

    public static List<Botanist> loadBotanists() throws IOException {
        File file = new File(BOTANISTS_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }

    public static List<Species> loadSpecies() throws IOException {
        File file = new File(SPECIES_PATH);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }
}
