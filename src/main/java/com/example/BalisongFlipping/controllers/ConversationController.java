package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.messagingDtos.SendMessageDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/conversations")
@RestController
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    @Autowired private ConversationService conversationService;
    @Autowired private AccountService accountService;

    // Inbox — all conversations for the logged-in user
    @GetMapping("/me")
    public ResponseEntity<?> getInbox() {
        try {
            String accountId = accountService.getSelf().id();
            return ResponseEntity.ok(conversationService.getInbox(accountId));
        } catch (Exception e) {
            log.error("GET /conversations/me -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Paginated message history for a conversation
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return ResponseEntity.ok(conversationService.getMessages(conversationId, accountId, page, size));
        } catch (Exception e) {
            log.error("GET /conversations/{}/messages -> {}", conversationId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Send a message — creates the conversation if it doesn't exist yet
    @PostMapping("/{recipientId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable("recipientId") String recipientId,
            @RequestBody SendMessageDto dto
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(
                    conversationService.sendMessage(accountId, recipientId, dto.body()),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            log.error("POST /conversations/{}/messages -> {}", recipientId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Mark all messages in a conversation as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable("id") Long conversationId) {
        try {
            String accountId = accountService.getSelf().id();
            conversationService.markRead(conversationId, accountId);
            return ResponseEntity.ok(Map.of("message", "Conversation marked as read."));
        } catch (Exception e) {
            log.error("PATCH /conversations/{}/read -> {}", conversationId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // Soft-delete conversation for the requester
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable("id") Long conversationId) {
        try {
            String accountId = accountService.getSelf().id();
            conversationService.deleteConversation(conversationId, accountId);
            return ResponseEntity.ok(Map.of("message", "Conversation deleted."));
        } catch (Exception e) {
            log.error("DELETE /conversations/{} -> {}", conversationId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
