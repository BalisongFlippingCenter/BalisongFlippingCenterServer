package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.catalogDtos.KnifeDetailDto;
import com.example.BalisongFlipping.dtos.catalogDtos.KnifeSummaryDto;
import com.example.BalisongFlipping.dtos.catalogDtos.MakerDetailDto;
import com.example.BalisongFlipping.services.KnifeCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/catalog")
@RestController
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private KnifeCatalogService knifeCatalogService;

    @GetMapping("/any/knives")
    public ResponseEntity<?> searchKnives(@RequestParam(value = "search", required = false) String search) {
        try {
            List<KnifeSummaryDto> results = knifeCatalogService.searchKnives(search);
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /catalog/any/knives?search={} -> {}", search, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/any/knives/{slug}")
    public ResponseEntity<?> getKnife(@PathVariable("slug") String slug) {
        try {
            KnifeDetailDto result = knifeCatalogService.getKnifeBySlug(slug);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("GET /catalog/any/knives/{} -> {}", slug, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/any/makers/{slug}")
    public ResponseEntity<?> getMaker(@PathVariable("slug") String slug) {
        try {
            MakerDetailDto result = knifeCatalogService.getMakerBySlug(slug);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("GET /catalog/any/makers/{} -> {}", slug, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
