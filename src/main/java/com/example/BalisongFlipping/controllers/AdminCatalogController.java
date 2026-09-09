package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.catalogSeedDtos.CatalogImportDto;
import com.example.BalisongFlipping.dtos.catalogSeedDtos.MakerSeedDto;
import com.example.BalisongFlipping.seed.CatalogSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All endpoints here are restricted to ROLE_ADMIN -- see
// SecurityFilterConfig's /admin/catalog/** rule.
@RequestMapping("/admin/catalog")
@RestController
public class AdminCatalogController {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogController.class);

    @Autowired
    private CatalogSeedService catalogSeedService;

    @PostMapping("/import")
    public ResponseEntity<?> importCatalog(@RequestBody CatalogImportDto dto) {
        try {
            catalogSeedService.importCatalog(
                    dto.makers() != null ? dto.makers() : List.of(),
                    dto.knives() != null ? dto.knives() : List.of()
            );
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /admin/catalog/import -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/makers")
    public ResponseEntity<?> createMaker(@RequestBody MakerSeedDto dto) {
        try {
            return new ResponseEntity<>(catalogSeedService.createMaker(dto), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("POST /admin/catalog/makers -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PutMapping("/makers/{slug}")
    public ResponseEntity<?> updateMaker(@PathVariable("slug") String slug, @RequestBody MakerSeedDto dto) {
        try {
            return new ResponseEntity<>(catalogSeedService.updateMaker(slug, dto), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("PUT /admin/catalog/makers/{} -> {}", slug, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/makers/{slug}")
    public ResponseEntity<?> deleteMaker(@PathVariable("slug") String slug) {
        try {
            catalogSeedService.deleteMaker(slug);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("DELETE /admin/catalog/makers/{} -> {}", slug, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
