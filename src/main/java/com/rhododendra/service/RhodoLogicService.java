package com.rhododendra.service;

import com.rhododendra.model.ResolvedPhotoDetails;
import com.rhododendra.service.SearchService.RhodoIndexResults;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {
    public final static List<String> ALPHABET = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");


    public static RhodoIndexResults scrollRhodosByLetter(String letter, int pageSize, int offset) throws IOException {
        // todo validate input
        var result = SearchService.getAllRhodosByFirstLetter(letter.toLowerCase(), pageSize, offset);
        result.rhodos.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
    }

    public record NextPage(
        String letter,
        int offset
    ) {
    }

    public static NextPage calculateNextPage(RhodoIndexResults currentPage) {
        if (currentPage.indexPagePos >= currentPage.indexPages.size() - 1) { // last page for letter.
            if (currentPage.letter.equalsIgnoreCase(ALPHABET.get(ALPHABET.size() - 1))) { // last letter in alphabet.
                return null;
            } else {
                var nextLetterPosition = ALPHABET.indexOf(currentPage.letter.toUpperCase()) + 1;
                return new NextPage(
                    ALPHABET.get(nextLetterPosition),
                    0
                );
            }
        } else {
            return new NextPage(
                currentPage.letter,
                currentPage.indexPages.get(currentPage.indexPagePos).endPos + 1
            );
        }
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
