package com.rhododendra.controller;

import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class WebController {
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/search")
    public String test(Model model, @RequestParam("q") String query) throws IOException, ParseException {
        model.addAttribute(
            "search_results",
            SearchService.searchSpecies(query)
        );

        return "search-results";
    }
}