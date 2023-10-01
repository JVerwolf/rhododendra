package com.rhododendra.service;

import com.rhododendra.model.Species;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public class Rhodos {
    public static List<Species> searchSpecies(String queryString) throws IOException, ParseException {
        return SearchService.searchSpecies(queryString).stream()
            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }

    public static List<Species> getSpeciesById(String id) throws IOException {
        return SearchService.getSpeciesById(id).stream()
            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }
}
