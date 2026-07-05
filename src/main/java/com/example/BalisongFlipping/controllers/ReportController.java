package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.reportDtos.CreateReportDto;
import com.example.BalisongFlipping.dtos.reportDtos.UpdateReportStatusDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired private ReportService reportService;

    @PostMapping
    public ResponseEntity<?> submitReport(@RequestBody CreateReportDto dto) {
        try {
            Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return ResponseEntity.ok(reportService.submitReport(account.getId(), dto));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(reportService.getReports(status, targetType, page, size));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody UpdateReportStatusDto dto) {
        try {
            Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return ResponseEntity.ok(reportService.updateStatus(id, account.getId(), dto.status()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
