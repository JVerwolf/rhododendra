package com.rhododendra.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rhododendra.config.Settings;
import com.rhododendra.model.ResolvedPhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.model.Rhododendron.SearchFilters;
import com.rhododendra.service.SearchService.IndexResults;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public class RhodoLogicService {
    public final static List<String> UPPER_CASE_ALPHABET = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");

    public static IndexResults<Rhododendron> scrollRhodosByLetter(
        String letter,
        int pageSize,
        int offset,
        boolean onlyPics,
        List<SearchFilters> searchFilters
    ) throws IOException {
        // todo validate input
        var result = SearchService.getAllRhodosByFirstLetter(
            letter.toLowerCase(),
            pageSize,
            offset,
            onlyPics,
            searchFilters
        );
        result.results.forEach(rhodo -> {
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()));
                addSelectedSpecies(rhodo);
            }
        );
        return result;
    }

    public static void addSelectedSpecies(Rhododendron rhodo) {
        if (rhodo.getSpecies_id() != null) {
            var original_species = SearchService.getRhodoById(rhodo.getSpecies_id());
            if (!original_species.isEmpty()) {
                rhodo.setSelectedSpecies(original_species.get(0));
            }
        }
    }

    public static IndexResults<Rhododendron> searchRhodos(String queryString, int pageSize, int offset) throws IOException, ParseException {
        // todo validate input
        var result = SearchService.searchRhodos(queryString, pageSize, offset);
        result.results.forEach(rhodo -> {
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()));
                addSelectedSpecies(rhodo);
            }
        );
        return result;
    }

    public static IndexResults<Rhododendron> rhodoParentageSearch(
        String seedParent,
        String pollenParent,
        boolean requireSeed,
        boolean requirePollen,
        boolean allowReverse,
        String originalRhodoId,
        int pageSize,
        int offset
    ) throws IOException {
        // todo validate input
        var result = SearchService.searchByParentage(seedParent, pollenParent, requireSeed, requirePollen, allowReverse, originalRhodoId, pageSize, offset);
        result.results.forEach(rhodo -> {
                rhodo.setPhotos(ImageResolver.resolveImages(rhodo.getPhotos()));
                addSelectedSpecies(rhodo);
            }
        );
        return result;
    }


    public static String getFormattedSeedParentName(Rhododendron rhodo) {
        if (rhodo.getSeedParentId() != null) {
            return getFormattedRhodoName(rhodo.getSeedParentId());
        } else if (rhodo.getParentage() != null && rhodo.getParentage().getSeed_parent() != null) {
            return formatNameString(rhodo.getParentage().getSeed_parent()); // TODO: this may not be right, could be a hybrid
        }
        return "?";
    }

    public static String getFormattedPollenParentName(Rhododendron rhodo) {
        if (rhodo.getPollenParentId() != null) {
            return getFormattedRhodoName(rhodo.getPollenParentId());
        } else if (rhodo.getParentage() != null && rhodo.getParentage().getPollen_parent() != null) {
            return formatNameString(rhodo.getParentage().getPollen_parent()); // TODO: this may not be right, could be a hybrid
        }
        return "?";
    }


    public static String getFormattedRhodoName(String id) {
        var result = SearchService.getRhodoById(id);
        if (!result.isEmpty()) {
            return getFormattedRhodoName(result.get(0));
        } else {
            return "";
        }
    }

    // TODO write function to format parentage name strings as a fallback.
    //  - write String format  function, then have the function below pass in the raw name if it there's no parent ids as a fallback.


    public static String getFormattedRhodoName(Rhododendron rhodo) {
        if (rhodo == null) return "";
        var sb = new StringBuilder("<i>R. </i>");

        if (rhodo.isSpecies() && !rhodo.getIs_species_selection()) {
            formatSpeciesNames(sb, rhodo.getName());
        } else if (rhodo.getIs_species_selection()) {
            var speciesResult = SearchService.getRhodoById(rhodo.getSpecies_id());
            if (!speciesResult.isEmpty()) {
                formatSpeciesNames(sb, speciesResult.get(0).getName());
            }
        }
        if (rhodo.isCultivar() || rhodo.getIs_species_selection()) {
            sb.append("'");
            sb.append(rhodo.getName());
            sb.append("'");
        }
        return sb.toString();
    }

    /**
     * Does not add R., as names can start with Azalea or R.
     */
    public static String formatSynonymName(String name) {
        if (name == null) return "";
        var sb = new StringBuilder();
        formatSpeciesNames(sb, name);
        return sb.toString();
    }

    /**
     * Adds an R.
     */
    public static String formatNameString(String name) {
        if (name == null) return "";
        var sb = new StringBuilder("<i>R. </i>");
        formatSpeciesNames(sb, name);
        return sb.toString();
    }

    private static void formatSpeciesNames(StringBuilder sb, String name) {
        var dontFormat = List.of("var", "var.", "subs", "subs.", "ssp", "ssp.",
            "f", "f.", "forma", "?", "x", "X");
        var italicize = List.of("R.", "Rhododendron", "A.", "Azalea");
        for (String token : name.split(" ")) {
            if (italicize.contains(token)) {
                sb.append("<i>");
                sb.append(token);
                sb.append(" </i>");
            } else if (isFirstLetterUpperCased(token) || dontFormat.contains(token)) {
                sb.append(token);
                sb.append(" ");
            } else {
                sb.append("<i>");
                sb.append(token);
                sb.append(" </i>");
            }
        }
    }

    public static boolean isFirstLetterUpperCased(String string) {
        if (!string.isEmpty()) {
            return UPPER_CASE_ALPHABET.contains(string.substring(0, 1));
        }
        return false;
    }


    public record NextIndexPage(
        String letter,
        int offset
    ) {
    }

    public static <T> NextIndexPage calculateNextIndexPage(IndexResults<T> currentPage, String letter) {
        if (currentPage.indexPagePos >= currentPage.indexPages.size() - 1) { // last page for letter.
            if (letter.equalsIgnoreCase(UPPER_CASE_ALPHABET.get(UPPER_CASE_ALPHABET.size() - 1))) { // last letter in alphabet.
                return null;
            } else {
                var nextLetterPosition = UPPER_CASE_ALPHABET.indexOf(letter.toUpperCase()) + 1;
                return new NextIndexPage(
                    UPPER_CASE_ALPHABET.get(nextLetterPosition),
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

    public static List<String> getAllRhodoDetailPages() {
        return SearchService.getAllRhodoIds().stream()
            .map(id -> Settings.DOMAIN + "/rhodos/" + id)
            .toList();
    }
}
