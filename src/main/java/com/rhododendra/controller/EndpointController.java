package com.rhododendra.controller;

import com.rhododendra.model.SearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EndpointController {

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProduct(@RequestParam("q") String query) {
        System.out.println(query);
        return new ResponseEntity<>(
            new SearchResult(),
            HttpStatus.OK
        );
    }
}