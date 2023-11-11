package com.rhododendra.controller;

import com.rhododendra.service.ImageResolver;
import com.rhododendra.service.RhodoLogicService;
import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.stream.IntStream;

import static com.rhododendra.model.Rhododendron.RhodoDataType.SPECIES_SELECTION;
import static com.rhododendra.service.RhodoLogicService.ALPHABET;

@Controller
public class WebController {

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
        @RequestParam(value = "justPics", defaultValue = "false") boolean justPics
    ) throws IOException {
        var set_size = 50;
        var results = RhodoLogicService.scrollRhodosByLetter(letter, set_size, offset,justPics);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("currentLetter", letter)
            .addAttribute("justPics", justPics)
            .addAttribute("pageSize", set_size)
            .addAttribute("nextPage", RhodoLogicService.calculateNextIndexPage(results, letter))
            .addAttribute("letters", ALPHABET);
        return "rhodo-index";
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
            if (rhodo.getRhodoDataType() == SPECIES_SELECTION) {
                var speciesResult = SearchService.getRhodoById(rhodo.getSpecies_id());
                if (!speciesResult.isEmpty()) {
                    model.addAttribute("original_species", speciesResult.get(0));

                }
            }
            return "rhodo-detail";
        } else {
            return "404";
        }
    }
}
