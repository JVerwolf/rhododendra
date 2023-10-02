package com.rhododendra.service;

import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Species;
import com.rhododendra.model.SpeciesDetail;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {
    public static List<Species> searchSpecies(String queryString) throws IOException, ParseException {
        return SearchService.searchSpecies(queryString).stream()
            .peek(rhodo ->
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
            )
            .toList();
    }

    public static SpeciesDetail getSpeciesDetailForView(String id) throws IOException {
        var rawSpecies = SearchService.getSpeciesById(id);
        if (!rawSpecies.isEmpty()) {
            var species = rawSpecies.get(0);
            var photoRowInfoList = species
                .getPhotos()
                .stream()
                .flatMap((imgFileName) ->
                    SearchService.getPhotoDetailsById(imgFileName)
                        .stream()
                        .map((photoDetails) ->
                            new SpeciesDetail.PhotoRowInfo(
                                ImageResolver.resolveImagePath(imgFileName),
                                photoDetails
                            )
                        )
                )
                .toList();
            return new SpeciesDetail(species, photoRowInfoList);
        } else {
            return null;
        }
    }

    public static List<Species> getSpeciesById(String id) throws IOException {
        return SearchService.getSpeciesById(id).stream()
            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }

    public static List<PhotoDetails> getPhotoDetails(List<String> photoLinks) throws IOException {
        return SearchService.getMultiplePhotoDetailsById(photoLinks)
            .stream()
            .filter(details -> !details.isEmpty())
            .map(details -> details.get(0))
            .toList();
    }
}