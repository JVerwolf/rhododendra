package com.rhododendra.service;

import com.rhododendra.model.*;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {

    public static List<Species> scrollSpeciesByLetter(String letter, int pageSize, int offset) throws IOException {
        // todo validate input
        return SearchService.getAllSpeciesByFirstLetter(letter.toLowerCase()).stream()
            .peek(rhodo ->
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
            )
            .toList();
    }

    public static SearchService.IndexResults scrollHybridsByLetter(String letter, int pageSize, int offset) throws IOException {
        // todo validate input
        var result = SearchService.getAllHybridsByFirstLetter(letter.toLowerCase(), pageSize, offset);
        result.hybrids.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
    }

    public static SearchService.RhodoIndexResults scrollRhodosByLetter(String letter, int pageSize, int offset) throws IOException {
        // todo validate input
        var result = SearchService.getAllRhodosByFirstLetter(letter.toLowerCase(), pageSize, offset);
        result.rhodos.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
    }

    public static List<Species> searchSpecies(String queryString) throws IOException, ParseException {
        return SearchService.searchSpecies(queryString).stream()
            .peek(rhodo ->
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
            )
            .toList();
    }

    public static List<Rhododendron> searchRhodos(String queryString) {
        return SearchService.searchRhodos(queryString).stream()
            .peek(rhodo ->
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
            )
            .toList();
    }

    public static List<Hybrid> searchHybrids(String queryString) throws IOException, ParseException {
        return SearchService.searchHybrids(queryString).stream()
            .peek(rhodo ->
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
            )
            .toList();
    }

    public static SpeciesDetail getSpeciesDetailForView(String id) {
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
                                ImageResolver.resolveImagePath(photoDetails.getHiResPhoto()),
                                ImageResolver.resolveImagePath(photoDetails.getTag()),
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

    public static List<Species> getSpeciesById(String id) {
        return SearchService.getSpeciesById(id).stream()
//            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }

    public static List<Hybrid> getHybridById(String id) {
        return SearchService.getHybridById(id).stream()
//            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }

    public static List<Rhododendron> getRhodoById(String id) {
        return SearchService.getRhodoById(id).stream()
//            .peek(rhodo -> rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos())))
            .toList();
    }

    public static List<ResolvedPhotoDetails> getResolvedPhotoDetails(List<String> ids) {
        return SearchService.getMultiplePhotoDetailsById(ids)
            .stream()
            .map(photoDetails ->
                new ResolvedPhotoDetails(
                    ImageResolver.resolveImagePath(photoDetails.getPhoto()),
                    ImageResolver.resolveImagePath(photoDetails.getHiResPhoto()),
                    ImageResolver.resolveImagePath(photoDetails.getTag()),
                    photoDetails
                ))
            .toList();
    }


}
