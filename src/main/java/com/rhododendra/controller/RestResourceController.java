package com.rhododendra.controller;

import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@RestController
public class RestResourceController {

    @GetMapping(value = "/search_json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchJson(@RequestParam("q") String query) throws IOException, ParseException {
        System.out.println(query);

        return new ResponseEntity<>(
            SearchService.searchSpecies(query),
            HttpStatus.OK
        );
    }

    /**
     * This is just for local hosting and testing.
     */
    @GetMapping(value = "/img/{id:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getImage(@PathVariable("id") String id) throws IOException {
        System.out.println(id);
        final ByteArrayResource inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
            "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species_photos/" + id
        )));
        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_JPEG)
            .contentLength(inputStream.contentLength())
            .body(inputStream);
    }
}