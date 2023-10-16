package com.rhododendra.controller;

import com.rhododendra.service.RhodoLogicService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class WebController {
    public String index() {
        return "index";
    }

    final static String[] ALPHABET = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    @RequestMapping("/species_index")
    public String handleSpeciesIndex(
        Model model,
        @RequestParam("letter") String letter,
        @RequestParam("size") int size,
        @RequestParam("offset") int offset
    ) throws IOException {
        model.addAttribute(
                "species",
                RhodoLogicService.scrollSpeciesByLetter(letter, size, offset)
            )
            .addAttribute(
                "letters",
                ALPHABET
            );
        return "species-index";
    }

    @RequestMapping("/hybrid_index")
    public String handleHybridIndex(
        Model model,
        @RequestParam("letter") String letter,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam("offset") int offset
    ) throws IOException {
        var set_size = 50;
        var results = RhodoLogicService.scrollHybridsByLetter(letter, set_size, offset);
        model.addAttribute("hybrids", results.hybrids)
            .addAttribute("indexPages", results.indexPages)
            .addAttribute("indexPagePos", results.indexPagePos)
            .addAttribute("currentLetter", letter)
            .addAttribute("pageSize", set_size)
            .addAttribute("letters", ALPHABET);
        return "hybrid-index";
    }

    @RequestMapping(value = "/search")
    public String handleSearch(Model model, @RequestParam("q") String query) throws IOException, ParseException {
        model.addAttribute(
            "search_results",
            RhodoLogicService.searchSpecies(query)
        );
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

    @RequestMapping(value = "/rhodos/{id}")
    public String handleGetHybrid(Model model, @PathVariable("id") String id) {
        if (id.startsWith("s")) {
            var speciesDetail = RhodoLogicService.getSpeciesDetailForView(id);
            if (speciesDetail != null) {
                model.addAttribute("speciesDetail", speciesDetail);
                return "species-detail";
            } else {
                return "404";
            }
        } else if (id.startsWith("h")) {
            var result = RhodoLogicService.getHybridById(id);
            if (!result.isEmpty()) {
                var hybrid = result.get(0);
                model.addAttribute("hybrid", hybrid);
                model.addAttribute("resolvedPhotoDetails", RhodoLogicService.getResolvedPhotoDetails(hybrid.getPhotos()));
                if (hybrid.getIs_species_selection()) {
                    var speciesResult = RhodoLogicService.getSpeciesById(hybrid.getSpecies_id());
                    if (!speciesResult.isEmpty()) {
                        model.addAttribute("original_species", speciesResult.get(0));

                    }
                }
                return "hybrid-detail";
            } else {
                return "404";
            }
        } else {
            return "404";
        }
    }
}
