package com.rhododendra.controller;

import com.rhododendra.model.SearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EndpointController {

    @GetMapping(value = "/search")
    public ResponseEntity<Object> getProduct() {
        return new ResponseEntity<>(new SearchResult(), HttpStatus.OK);
    }
}