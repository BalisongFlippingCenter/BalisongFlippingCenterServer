package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long accountId = currentAccountId();
            return ResponseEntity.ok(notificationService.getNotifications(accountId, unreadOnly, page, size));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            return ResponseEntity.ok(notificationService.getUnreadCount(currentAccountId()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(notificationService.markRead(id, currentAccountId()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead() {
        try {
            notificationService.markAllRead(currentAccountId());
            return ResponseEntity.ok("All notifications marked as read.");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    private Long currentAccountId() {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return account.getId();
    }
}
