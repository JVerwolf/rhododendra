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
                new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"}
            );
        return "species-index";
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
    public String handleAbout() throws IOException, ParseException {
        return "about";
    }

    @RequestMapping(value = "/species/{id}")
    public String handleGetSpecies(Model model, @PathVariable("id") String id) throws IOException {
        var speciesDetail = RhodoLogicService.getSpeciesDetailForView(id);
        if (speciesDetail != null) {
            model.addAttribute("speciesDetail", speciesDetail);
            return "species-detail";
        } else {
            return "404";
        }
    }
}