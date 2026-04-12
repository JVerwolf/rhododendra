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

import com.rhododendra.config.AppSettings;
import com.rhododendra.db.RhododendronRepository;
import com.rhododendra.model.Rhododendron.SearchFilters;
import com.rhododendra.service.RhodoLogicService;
import com.rhododendra.service.SearchService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.rhododendra.security.PostLoginRedirect;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.rhododendra.service.RhodoLogicService.UPPER_CASE_ALPHABET;

@Controller
public class WebController {
    AppSettings appSettings;
    RhododendronRepository rhododendronRepository;

    public WebController(
        AppSettings appSettings,
        RhododendronRepository rhododendronRepository
    ) {
        this.appSettings = appSettings;
        this.rhododendronRepository = rhododendronRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    private static boolean isLegacyId(String id) {
        return id == null || id.isBlank() || !id.chars().allMatch(Character::isDigit);
    }

    private static Long parseNumericId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return null;
        }
    }

    private static RedirectView permanentRedirectTo(String target) {
        RedirectView redirectView = new RedirectView(target);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("domain", appSettings.domain);
        return "home";
    }

    @RequestMapping("/login")
    public String login(
        Model model,
        @RequestParam(name = "continue", required = false) String continueTarget,
        HttpSession session
    ) {
        if (!appSettings.isSignInEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("domain", appSettings.domain);
        if (continueTarget != null && PostLoginRedirect.isSafeRedirect(continueTarget)) {
            session.setAttribute(PostLoginRedirect.SESSION_ATTRIBUTE, continueTarget);
        } else {
            session.removeAttribute(PostLoginRedirect.SESSION_ATTRIBUTE);
        }
        return "login";
    }

    @RequestMapping("/rhodo_index")
    public String handleRhodoIndex(
        Model model,
        @RequestParam(value = "letter", defaultValue = "a") String letter,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "justPics", defaultValue = "false") boolean justPics,
        @RequestParam(value = "searchFilters", required = false) List<SearchFilters> searchFilters
    ) throws IOException {
        var set_size = 50;
        var results = RhodoLogicService.scrollRhodosByLetter(
            letter,
            set_size,
            offset,
            justPics,
            searchFilters
        );
        Set<String> searchFilterStrings = (searchFilters == null)
            ? Set.of()
            : searchFilters.stream().map(Enum::name).collect(Collectors.toSet());

        model
            .addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("currentLetter", letter)
            .addAttribute("justPics", justPics)
            .addAttribute("searchFilters", searchFilterStrings)
            .addAttribute("pageSize", set_size)
            .addAttribute("nextPage", RhodoLogicService.calculateNextIndexPage(results, letter))
            .addAttribute("letters", UPPER_CASE_ALPHABET)
            .addAttribute("domain", appSettings.domain);
        return "rhodo-index";
    }

    public enum UseCase {
        SIBLINGS,
        CHILDREN
    }

    @RequestMapping("/genetic_search")
    public String handleGeneticSearch(
        Model model,
        @RequestParam(value = "seedParentId", required = false) Long seedParentId,
        @RequestParam(value = "pollenParentId", required = false) Long pollenParentId,
        @RequestParam(value = "requireSeed", defaultValue = "false") boolean requireSeed,
        @RequestParam(value = "requirePollen", defaultValue = "false") boolean requirePollen,
        @RequestParam(value = "ordered", defaultValue = "false") boolean ordered,
        @RequestParam(value = "originalRhodoId", required = false) Long originalRhodoId,
        @RequestParam(value = "useCase", required = true) UseCase usecase,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException {
        // TODO add logging.
        var set_size = 50;

        var originalRhodoList = SearchService.getRhodoById(originalRhodoId);
        var originalRhodo = !originalRhodoList.isEmpty() ? originalRhodoList.get(0) : null;

        var results = RhodoLogicService.rhodoParentageSearch(seedParentId, pollenParentId, requireSeed, requirePollen, !ordered, originalRhodoId, set_size, offset);
        var formattedName = RhodoLogicService.getFormattedRhodoName(originalRhodo);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("seedParentFormattedName", RhodoLogicService.getFormattedRhodoName(seedParentId))
            .addAttribute("seedParentId", seedParentId)
            .addAttribute("pollenParentFormattedName", RhodoLogicService.getFormattedRhodoName(pollenParentId))
            .addAttribute("pollenParentId", pollenParentId)
            .addAttribute("originalRhodoId", originalRhodoId)
            .addAttribute("originalRhodoFormattedName", formattedName.replace("<i>", "").replace("</i>", ""))
            .addAttribute("originalRhodoFormattedNameForHead", formattedName)
            .addAttribute("requireSeed", requireSeed)
            .addAttribute("requirePollen", requirePollen)
            .addAttribute("ordered", ordered)
            .addAttribute("useCase", usecase.name())
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray())
            .addAttribute("domain", appSettings.domain);
        return "genetic-search";
    }

    @RequestMapping("/taxonomic_search")
    public String handleTaxonomicSearch(
        Model model,
        @RequestParam(value = "subgenus", required = false) String subgenus,
        @RequestParam(value = "section", required = false) String section,
        @RequestParam(value = "subsection", required = false) String subsection,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        var set_size = 50;
        var results = RhodoLogicService.rhodoTaxonomicSearch(subgenus, section, subsection, size, offset);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", set_size)
            .addAttribute("subgenus", subgenus)
            .addAttribute("section", section)
            .addAttribute("subsection", subsection)
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray())
            .addAttribute("domain", appSettings.domain);
        return "taxonomic-search";
    }

    @RequestMapping(value = "/search")
    public String handleSearch(
        Model model,
        @RequestParam("q") String query,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        var results = RhodoLogicService.searchRhodos(query, size, offset);
        model.addAttribute("rhodos", results.results)
            .addAttribute("resultPages", results.indexPages)
            .addAttribute("resultPagePos", results.indexPagePos)
            .addAttribute("pageSize", size)
            .addAttribute("query", query)
            .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray())
            .addAttribute("domain", appSettings.domain);
        return "search-results";
    }

    @RequestMapping(value = "/about")
    public String handleAbout(Model model) {
        model.addAttribute("domain", appSettings.domain);
        return "about";
    }

    @RequestMapping(value = "/contact")
    public String handleContact(Model model) {
        model.addAttribute("domain", appSettings.domain);
        return "contact";
    }

    @RequestMapping(value = "/links")

    public String handleLinks(Model model) {
        model.addAttribute("domain", appSettings.domain);
        return "links";
    }

    @RequestMapping(value = "/rhodos/{id}")
    public Object handleGetRhodo(Model model, @PathVariable("id") String id) {
        if (isLegacyId(id)) {
            var legacyResult = SearchService.getRhodoByOldId(id);
            if (!legacyResult.isEmpty()) {
                return permanentRedirectTo("/rhodos/" + legacyResult.get(0).getId());
            }
            logger.warn("Legacy rhodo requested but not found: {}", id);
            return "404";
        }
        Long numericId = parseNumericId(id);
        if (numericId == null) {
            logger.warn("Invalid rhodo id requested: {}", id);
            return "404";
        }
        var result = SearchService.getRhodoById(numericId);
        if (!result.isEmpty()) {
            var rhodo = result.get(0);
            model.addAttribute("rhodo", rhodo);

            var formattedName = rhodo.getFormattedName();
            model.addAttribute("rhodoFormattedName", formattedName);
            model.addAttribute("rhodoNameForHead", formattedName.replace("<i>", "").replace("</i>", ""));
            model.addAttribute("resolvedPhotoDetails", RhodoLogicService.getResolvedPhotoDetails(rhodo.getPhotos()));
            model.addAttribute("domain", appSettings.domain);
            if (rhodo.getIs_species_selection()) {
                var speciesResult = SearchService.getRhodoById(rhodo.getSpecies_id());
                if (!speciesResult.isEmpty()) {
                    model.addAttribute("original_species", speciesResult.get(0));
                }
            }
            return "rhodo-detail";
        } else {
            logger.warn("Rhodo requested but not found: " + id);
            return "404";
        }
    }

    @PostMapping("/rhodos/{id}/edit")
    public String saveRhodoEdit(
        @PathVariable("id") Long id,
        @RequestParam(value = "ten_year_height", required = false) String tenYearHeight,
        @RequestParam(value = "bloom_time", required = false) String bloomTime,
        @RequestParam(value = "flower_shape", required = false) String flowerShape,
        @RequestParam(value = "leaf_shape", required = false) String leafShape,
        @RequestParam(value = "colour", required = false) String colour,
        @RequestParam(value = "deciduous", required = false) String deciduous,
        @RequestParam(value = "hardiness", required = false) String hardiness,
        @RequestParam(value = "extra_information", required = false) String extraInformation,
        @RequestParam(value = "additional_parentage_info", required = false) String additionalParentageInfo,
        @RequestParam(value = "introduced", required = false) Integer introduced,
        @RequestParam(value = "first_described", required = false) String firstDescribed,
        @RequestParam(value = "origin_location", required = false) String originLocation,
        @RequestParam(value = "habit", required = false) String habit,
        @RequestParam(value = "observed_mature_height", required = false) String observedMatureHeight,
        @RequestParam(value = "azalea_group", required = false) String azaleaGroup,
        @RequestParam(value = "irrc_registered", required = false) String irrcRegistered,
        @RequestParam(value = "subgenus", required = false) String subgenus,
        @RequestParam(value = "section", required = false) String section,
        @RequestParam(value = "subsection", required = false) String subsection,
        RedirectAttributes redirectAttributes
    ) {
        var result = SearchService.getRhodoById(id);
        if (result.isEmpty()) {
            logger.warn("Rhodo edit requested but not found: {}", id);
            return "404";
        }
        try {
            int updated = rhododendronRepository.updateEditableFields(
                id,
                tenYearHeight,
                bloomTime,
                flowerShape,
                leafShape,
                colour,
                deciduous,
                hardiness,
                extraInformation,
                additionalParentageInfo,
                introduced,
                firstDescribed,
                originLocation,
                habit,
                observedMatureHeight,
                azaleaGroup,
                irrcRegistered,
                subgenus,
                section,
                subsection
            );
            if (updated == 0) {
                redirectAttributes.addFlashAttribute("editError", "No row updated.");
            } else {
                redirectAttributes.addFlashAttribute("editSuccess", true);
            }
        } catch (SQLException e) {
            logger.error("Failed to update rhododendron {}", id, e);
            redirectAttributes.addFlashAttribute("editError", "Could not save changes.");
        }
        return "redirect:/rhodos/" + id;
    }

    @RequestMapping(value = "/hybridizer/{id}")
    public Object handleGetHybridizer(
        Model model, @PathVariable("id") String hybridizerId,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "offset", defaultValue = "0") int offset
    ) throws IOException, ParseException {
        if (isLegacyId(hybridizerId)) {
            var legacyResult = SearchService.getHybridizerByOldId(hybridizerId);
            if (!legacyResult.isEmpty()) {
                return permanentRedirectTo("/hybridizer/" + legacyResult.get(0).getId());
            }
            logger.warn("Legacy hybridizer requested but not found: {}", hybridizerId);
            return "404";
        }
        Long numericId = parseNumericId(hybridizerId);
        if (numericId == null) {
            logger.warn("Invalid hybridizer id requested: {}", hybridizerId);
            return "404";
        }
        var set_size = 50;
        var result = SearchService.getHybridizerById(numericId);
        if (!result.isEmpty()) {
            var hybridizer = result.get(0);
            model.addAttribute("hybridizer", hybridizer);
            model.addAttribute("hybridizerResolvedPhotoDetails", RhodoLogicService.getResolvedPhotoDetails(hybridizer.getPhotos()));
            var results = RhodoLogicService.getRhodosByHybridizer(numericId, set_size, offset);
            model.addAttribute("rhodos", results.results)
                .addAttribute("resultPages", results.indexPages)
                .addAttribute("resultPagePos", results.indexPagePos)
                .addAttribute("pageSize", set_size)
                .addAttribute("id", numericId)
                .addAttribute("pageNumbers", IntStream.range(1, results.indexPages.size() + 1).toArray())
                .addAttribute("domain", appSettings.domain);

            return "hybridizer-detail";
        } else {
            logger.warn("Hybridizer requested but not found: " + hybridizerId);
            return "404";
        }
    }

    @RequestMapping(value = "/botanist/{id}")
    public Object handleGetBotanist(Model model, @PathVariable("id") String botanistId) {
        if (isLegacyId(botanistId)) {
            var legacyResult = SearchService.getBotanistByBotanicalShort(botanistId);
            if (!legacyResult.isEmpty()) {
                return permanentRedirectTo("/botanist/" + legacyResult.get(0).getId());
            }
            logger.warn("Legacy botanist requested but not found: {}", botanistId);
            return "404";
        }
        Long numericId = parseNumericId(botanistId);
        if (numericId == null) {
            logger.warn("Invalid botanist id requested: {}", botanistId);
            return "404";
        }
        var result = SearchService.getBotanistById(numericId);
        if (result.isEmpty()) {
            logger.warn("Botanist requested but not found: {}", numericId);
            return "404";
        }
        model.addAttribute("botanist", result.get(0));
        model.addAttribute("domain", appSettings.domain);
        return "botanist-detail";
    }
}
