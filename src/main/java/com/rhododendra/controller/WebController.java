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
        @RequestParam("letter") String letter,
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

    @RequestMapping("/parentage_search")
    public String handleParentageSearch(
        Model model,
        @RequestParam(value = "seedParentId", required = false) String seedParentId,
        @RequestParam(value = "pollenParentId", required = false) String pollenParentId,
        @RequestParam(value = "mustMatchParentage", defaultValue = "false") boolean mustMatchParentage,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException {
        // TODO add logging.
        var set_size = 50;

        var seedParentList = SearchService.getRhodoById(seedParentId);
        var seedParent = !seedParentList.isEmpty() ? seedParentList.get(0) : null;

        var pollenParentList = SearchService.getRhodoById(pollenParentId);
        var pollenParent = !pollenParentList.isEmpty() ? pollenParentList.get(0) : null;

        var results = RhodoLogicService.rhodoParentageSearch(seedParentId, pollenParentId, mustMatchParentage, set_size, offset);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("seedParent", seedParent)
            .addAttribute("pollenParent", pollenParent)
            .addAttribute("mustMatchParentage", mustMatchParentage)
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray());
        return "parentage_search";
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
    public String handleGetHybrid(Model model, @PathVariable("id") String id) {
        var result = SearchService.getRhodoById(id);
        if (!result.isEmpty()) {
            var rhodo = result.get(0);
            model.addAttribute("rhodo", rhodo);
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
}
