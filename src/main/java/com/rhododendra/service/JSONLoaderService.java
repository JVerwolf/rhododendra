package com.rhododendra.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhododendra.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class JSONLoaderService {

    private final File botanistsPath;
    private final File speciesPath;
    private final File hybridsPath;
    private final File azaleasPath;
    private final File vireyasPath;
    private final File hybridizerDetailsPath;
    private final File azaleodendronsPath;
    private final File photoDetailsPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JSONLoaderService(@Value("${data.jsonDir}") String jsonDir) {
        File base = new File(jsonDir);
        this.botanistsPath = new File(base, "species_botanists.json");
        this.speciesPath = new File(base, "species.json");
        this.hybridsPath = new File(base, "hybrids.json");
        this.azaleasPath = new File(base, "azaleas.json");
        this.vireyasPath = new File(base, "vireyas.json");
        this.hybridizerDetailsPath = new File(base, "hybridizers.json");
        this.azaleodendronsPath = new File(base, "azaleodendrons.json");
        this.photoDetailsPath = new File(base, "photo_details.json");
    }

    public List<Botanist> loadBotanists() throws IOException {
        return objectMapper.readValue(botanistsPath, new TypeReference<>() {});
    }

    public List<Rhododendron> loadRhodos() throws IOException {
        List<Rhododendron> result = objectMapper.readValue(speciesPath, new TypeReference<List<Rhododendron>>() {});
        result.addAll(objectMapper.readValue(hybridsPath, new TypeReference<List<Rhododendron>>() {}));
        result.addAll(objectMapper.readValue(azaleasPath, new TypeReference<List<Rhododendron>>() {}));
        result.addAll(objectMapper.readValue(vireyasPath, new TypeReference<List<Rhododendron>>() {}));
        result.addAll(objectMapper.readValue(azaleodendronsPath, new TypeReference<List<Rhododendron>>() {}));
        return result;
    }

    public List<PhotoDetails> loadPhotoDetails() throws IOException {
        return objectMapper.readValue(photoDetailsPath, new TypeReference<>() {});
    }

    public List<Hybridizer> loadHybridizers() throws IOException {
        return objectMapper.readValue(hybridizerDetailsPath, new TypeReference<>() {});
    }
}
