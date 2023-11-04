package com.rhododendra.service;

import com.rhododendra.model.ResolvedPhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.service.SearchService.IndexResults;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {
    public final static List<String> ALPHABET = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");


    public static IndexResults<Rhododendron> scrollRhodosByLetter(
        String letter,
        int pageSize,
        int offset,
        boolean onlyPics
    ) throws IOException {
        // todo validate input
        var result = SearchService.getAllRhodosByFirstLetter(letter.toLowerCase(), pageSize, offset, onlyPics);
        result.results.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
    }

    public static IndexResults<Rhododendron> searchRhodos(String queryString, int pageSize, int offset) throws IOException, ParseException {
        // todo validate input
        var result = SearchService.searchRhodos(queryString, pageSize, offset);
        result.results.forEach(rhodo ->
            rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()))
        );
        return result;
    }

    public record NextIndexPage(
        String letter,
        int offset
    ) {
    }

    public static <T> NextIndexPage calculateNextIndexPage(IndexResults<T> currentPage, String letter) {
        if (currentPage.indexPagePos >= currentPage.indexPages.size() - 1) { // last page for letter.
            if (letter.equalsIgnoreCase(ALPHABET.get(ALPHABET.size() - 1))) { // last letter in alphabet.
                return null;
            } else {
                var nextLetterPosition = ALPHABET.indexOf(letter.toUpperCase()) + 1;
                return new NextIndexPage(
                    ALPHABET.get(nextLetterPosition),
                    0
                );
            }
        } else {
            return new NextIndexPage(
                letter,
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
