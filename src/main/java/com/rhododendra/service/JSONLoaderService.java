package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.Botanist;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JSONLoaderService {
    final static String botanistsPath = "/Users/john.verwolf/code/hirsutum_scraper/outputs/species_botanists_table_data.json";

    public static List<Botanist> readBotanists() throws IOException {
        File file = new File(botanistsPath);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }
}
