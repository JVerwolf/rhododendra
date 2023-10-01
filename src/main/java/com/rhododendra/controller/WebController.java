package com.rhododendra.controller;

import com.rhododendra.service.ImageResolver;
import com.rhododendra.service.Rhodos;
import com.rhododendra.service.SearchService;
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

    @RequestMapping(value = "/search")
    public String handleSearch(Model model, @RequestParam("q") String query) throws IOException, ParseException {
        model.addAttribute(
            "search_results",
            Rhodos.searchSpecies(query)
        );
        return "search-results";
    }

    @RequestMapping(value = "/species/{id}")
    public String handleGetSpecies(Model model, @PathVariable("id") String id) throws IOException {
        var species = Rhodos.getSpeciesById(id);
        if (!species.isEmpty()) {
            model.addAttribute(
                "species",
                species.get(0)
            );
            return "species-detail";
        } else {
            return "404";
        }
    }
}