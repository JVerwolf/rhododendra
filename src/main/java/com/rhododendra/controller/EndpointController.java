package com.rhododendra.controller;

import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class EndpointController {

    @GetMapping(value = "/search_json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchJson(@RequestParam("q") String query) throws IOException, ParseException {
        System.out.println(query);

        return new ResponseEntity<>(
            SearchService.searchSpecies(query),
            HttpStatus.OK
        );
    }

//    @RequestMapping(value = "/pictures/normal/{id:.+}", method = RequestMethod.GET)
//    public ResponseEntity<byte[]> getImage(@PathVariable("id") String id) {
//        System.out.println(id);
////        byte[] image = imageService.getImage(id);
//        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
//    }
}