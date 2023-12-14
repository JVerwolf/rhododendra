package com.rhododendra.controller;

import com.rhododendra.model.Rhododendron.SearchFilters;
import com.rhododendra.service.RhodoLogicService;
import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.rhododendra.service.RhodoLogicService.UPPER_CASE_ALPHABET;

@Controller
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @RequestMapping("/")
    public String index() {
        return "home";
    }

    @RequestMapping("/rhodo_index")
    public String handleRhodoIndex(
        Model model,
        @RequestParam(value = "letter", defaultValue = "a") String letter,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "justPics", defaultValue = "false") boolean justPics,
        @RequestParam(value = "searchFilters", required = false) List<SearchFilters> searchFilters
    ) throws IOException {
        var set_size = 50;
        var results = RhodoLogicService.scrollRhodosByLetter(
            letter,
            set_size,
            offset,
            justPics,
            searchFilters
        );
        Set<String> searchFilterStrings = (searchFilters == null)
            ? Set.of()
            : searchFilters.stream().map(Enum::name).collect(Collectors.toSet());

        model
            .addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("currentLetter", letter)
            .addAttribute("justPics", justPics)
            .addAttribute("searchFilters", searchFilterStrings)
            .addAttribute("pageSize", set_size)
            .addAttribute("nextPage", RhodoLogicService.calculateNextIndexPage(results, letter))
            .addAttribute("letters", UPPER_CASE_ALPHABET);
        return "rhodo-index";
    }

    public enum UseCase {
        SIBLINGS,
        CHILDREN
    }

    @RequestMapping("/genetic_search")
    public String handleGeneticSearch(
        Model model,
        @RequestParam(value = "seedParentId", required = false) String seedParentId,
        @RequestParam(value = "pollenParentId", required = false) String pollenParentId,
        @RequestParam(value = "requireSeed", defaultValue = "false") boolean requireSeed,
        @RequestParam(value = "requirePollen", defaultValue = "false") boolean requirePollen,
        @RequestParam(value = "ordered", defaultValue = "false") boolean ordered,
        @RequestParam(value = "originalRhodoId", required = false) String originalRhodoId,
        @RequestParam(value = "useCase", required = true) UseCase usecase,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException {
        // TODO add logging.
        var set_size = 50;

        var originalRhodoList = SearchService.getRhodoById(originalRhodoId);
        var originalRhodo = !originalRhodoList.isEmpty() ? originalRhodoList.get(0) : null;

        var results = RhodoLogicService.rhodoParentageSearch(seedParentId, pollenParentId, requireSeed, requirePollen, !ordered, originalRhodoId, set_size, offset);
        var formattedName = RhodoLogicService.getFormattedRhodoName(originalRhodo);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("seedParentFormattedName", RhodoLogicService.getFormattedRhodoName(seedParentId))
            .addAttribute("seedParentId", seedParentId)
            .addAttribute("pollenParentFormattedName", RhodoLogicService.getFormattedRhodoName(pollenParentId))
            .addAttribute("pollenParentId", pollenParentId)
            .addAttribute("originalRhodoId", originalRhodoId)
            .addAttribute("originalRhodoFormattedName", formattedName.replace("<i>","").replace("</i>",""))
            .addAttribute("originalRhodoFormattedNameForHead", formattedName)
            .addAttribute("requireSeed", requireSeed)
            .addAttribute("requirePollen", requirePollen)
            .addAttribute("ordered", ordered)
            .addAttribute("useCase", usecase.name())
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray());
        return "genetic-search";
    }

    @RequestMapping("/taxonomic_search")
    public String handleTaxonomicSearch(
        Model model,
        @RequestParam(value = "subgenus", required = false) String subgenus,
        @RequestParam(value = "section", required = false) String section,
        @RequestParam(value = "subsection", required = false) String subsection,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        var set_size = 50;
        var results = RhodoLogicService.rhodoTaxonomicSearch(subgenus, section, subsection, size, offset);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("subgenus", subgenus)
            .addAttribute("section", section)
            .addAttribute("subsection", subsection)
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray());
        return "taxonomic-search";
    }

    @RequestMapping(value = "/search")
    public String handleSearch(
        Model model,
        @RequestParam("q") String query,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        var set_size = 50;
        var results = RhodoLogicService.searchRhodos(query, size, offset);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("query", query)
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray());
        return "search-results";
    }

    @RequestMapping(value = "/about")
    public String handleAbout() {
        return "about";
    }

    @RequestMapping(value = "/contact")
    public String handleContact() {
        return "contact";
    }

    @RequestMapping(value = "/links")
    public String handleLinks() {
        return "links";
    }

    @RequestMapping(value = "/rhodos/{id}")
    public String handleGetRhodo(Model model, @PathVariable("id") String id) {
        var result = SearchService.getRhodoById(id);
        if (!result.isEmpty()) {
            var rhodo = result.get(0);
            model.addAttribute("rhodo", rhodo);

            var formattedName = rhodo.getFormattedName();
            model.addAttribute("rhodoFormattedName", formattedName);
            model.addAttribute("rhodoNameForHead", formattedName.replace("<i>","").replace("</i>",""));
            model.addAttribute("resolvedPhotoDetails", RhodoLogicService.getResolvedPhotoDetails(rhodo.getPhotos()));
            if (rhodo.getIs_species_selection()) {
                var speciesResult = SearchService.getRhodoById(rhodo.getSpecies_id());
                if (!speciesResult.isEmpty()) {
                    model.addAttribute("original_species", speciesResult.get(0));
                }
            }
            return "rhodo-detail";
        } else {
            logger.warn("Rhodo requested but not found: " + id);
            return "404";
        }
    }

    @RequestMapping(value = "/hybridizer/{id}")
    public String handleGetHybridizer(
        Model model, @PathVariable("id") String hybridizerId,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        var set_size = 50;
        var result = SearchService.getHybridizerById(hybridizerId);
        if (!result.isEmpty()) {
            var hybridizer = result.get(0);
            model.addAttribute("hybridizer", hybridizer);
            model.addAttribute("hybridizerResolvedPhotoDetails", RhodoLogicService.getResolvedPhotoDetails(hybridizer.getPhotos()));
            var results = RhodoLogicService.getRhodosByHybridizer(hybridizerId,set_size, offset);
            model.addAttribute("rhodos", results.results)
                .addAttribute("resultPages", results.indexPages)
                .addAttribute("resultPagePos", results.indexPagePos)
                .addAttribute("pageSize", set_size)
                .addAttribute("id", hybridizerId)
                .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray());
            return "hybridizer-detail";
        } else {
            logger.warn("Hybridizer requested but not found: " + hybridizerId);
            return "404";
        }
    }
}
