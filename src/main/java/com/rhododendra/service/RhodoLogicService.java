package com.rhododendra.service;

import com.rhododendra.model.ResolvedPhotoDetails;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {

    public static SearchService.RhodoIndexResults scrollRhodosByLetter(String letter, int pageSize, int offset) throws IOException {
        // todo validate input
        var result = SearchService.getAllRhodosByFirstLetter(letter.toLowerCase(), pageSize, offset);
        result.rhodos.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
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
