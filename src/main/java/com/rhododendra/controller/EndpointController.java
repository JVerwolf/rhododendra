package com.rhododendra.controller;

import com.rhododendra.model.SearchResult;
import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class EndpointController {

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProduct(@RequestParam("q") String query) throws IOException, ParseException {
        System.out.println(query);

        return new ResponseEntity<>(
            SearchService.searchSpecies(query),
            HttpStatus.OK
        );
    }
}