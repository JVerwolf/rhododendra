/*
 * Rhododendra — Spring Boot web application for rhododendron data.
 * Copyright (C) 2026 Rhododendra contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.rhododendra.controller;

import com.rhododendra.service.RhodoLogicService;
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
    RhodoLogicService rhodoLogicService;

    public RestResourceController(RhodoLogicService rhodoLogicService) {
        this.rhodoLogicService = rhodoLogicService;
    }

    @GetMapping(value = {"/robots.txt", "/Robots.txt", "/robot.txt", "/Robot.txt"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() throws IOException {
        return ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body("Sitemap: https://rhododendra.com/sitemap.txt \n" +
                "Sitemap: https://rhododendra.com/all_rhodos_2.txt");
    }

    @GetMapping(value = {"all_rhodos_2.txt"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAllRhodos() {
        return ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(String.join("\n", rhodoLogicService.getAllRhodoDetailPages()));
    }

    /**
     * This is just for local hosting and testing.
     */
    @GetMapping(value = "/img/{id:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getImage(@PathVariable("id") String id) throws IOException {
//        System.out.println(id);
        final ByteArrayResource inputStream;
        if (id.startsWith("s")) {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/species_photos/" + id
            )));
        } else if (id.startsWith("h")) {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/hybrid_photos/" + id
            )));
        } else if (id.startsWith("a")) {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/azalea_photos/" + id
            )));
        } else if (id.startsWith("v")) {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/vireya_photos/" + id
            )));
        } else if (id.startsWith("ad")) {
            inputStream = new ByteArrayResource(Files.readAllBytes(Paths.get(
                "/Users/john.verwolf/code/hirsutum_scraper/outputs/data/azaleodendron_photos/" + id
            )));
        } else {
            throw new IOException("invalid image id: " + id);
        }
        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_JPEG)
            .contentLength(inputStream.contentLength())
            .body(inputStream);
    }
}