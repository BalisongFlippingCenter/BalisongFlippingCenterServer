package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.BanAccountDto;
import com.example.BalisongFlipping.dtos.MuteAccountDto;
import com.example.BalisongFlipping.dtos.SuspendAccountDto;
import com.example.BalisongFlipping.services.AdminAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// All endpoints here are restricted to ROLE_ADMIN -- see
// SecurityFilterConfig's /admin/accounts/** rule.
@RequestMapping("/admin/accounts")
@RestController
public class AdminAccountController {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountController.class);

    @Autowired
    private AdminAccountService adminAccountService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(adminAccountService.search(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminAccountService.getById(id));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<?> ban(@PathVariable String id, @RequestBody BanAccountDto dto) {
        try {
            return ResponseEntity.ok(adminAccountService.ban(id, dto.reason()));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/ban -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<?> unban(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminAccountService.unban(id));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/unban -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable String id, @RequestBody SuspendAccountDto dto) {
        try {
            return ResponseEntity.ok(adminAccountService.suspend(id, dto.reason(), dto.until()));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/suspend -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/unsuspend")
    public ResponseEntity<?> unsuspend(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminAccountService.unsuspend(id));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/unsuspend -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/mute")
    public ResponseEntity<?> mute(@PathVariable String id, @RequestBody MuteAccountDto dto) {
        try {
            return ResponseEntity.ok(adminAccountService.mute(id, dto.reason(), dto.until()));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/mute -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/unmute")
    public ResponseEntity<?> unmute(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminAccountService.unmute(id));
        } catch (Exception e) {
            log.error("POST /admin/accounts/{}/unmute -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
