package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.ConversationService;
import com.example.BalisongFlipping.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RequestMapping("/conversations")
@RestController
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;   // 10 MB
    private static final long MAX_VIDEO_BYTES = 150L * 1024 * 1024; // 150 MB

    @Value("${cloud.aws.s3.bucket}")  private String bucketName;
    @Value("${cloud.aws.region.static}") private String s3Region;

    @Autowired private ConversationService conversationService;
    @Autowired private AccountService accountService;
    @Autowired private S3Service s3Service;

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

    // Send a message — creates the conversation if it doesn't exist yet.
    // Accepts multipart/form-data: optional text `body` + optional `mediaFile` (image or video).
    @PostMapping(value = "/{recipientId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessage(
            @PathVariable("recipientId") String recipientId,
            @RequestParam(value = "body", required = false) String body,
            @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile
    ) {
        try {
            String accountId = accountService.getSelf().id();

            String mediaUrl = null;
            boolean isVideo = false;

            if (mediaFile != null && !mediaFile.isEmpty()) {
                String contentType = mediaFile.getContentType() != null ? mediaFile.getContentType() : "";
                if (!contentType.startsWith("image/") && !contentType.startsWith("video/")) {
                    return new ResponseEntity<>("Only image and video files are allowed.", HttpStatus.BAD_REQUEST);
                }
                isVideo = contentType.startsWith("video/");
                long maxBytes = isVideo ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
                if (mediaFile.getSize() > maxBytes) {
                    String limit = isVideo ? "150 MB" : "10 MB";
                    return new ResponseEntity<>("File exceeds the " + limit + " limit.", HttpStatus.BAD_REQUEST);
                }

                String originalFilename = mediaFile.getOriginalFilename() != null
                        ? mediaFile.getOriginalFilename() : "file";
                String key = "messages/" + accountId + "/" + UUID.randomUUID() + "/" + originalFilename;
                s3Service.uploadFile(bucketName, key, mediaFile.getSize(), contentType, mediaFile.getInputStream());
                mediaUrl = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
            }

            return new ResponseEntity<>(
                    conversationService.sendMessage(accountId, recipientId, body, mediaUrl, isVideo),
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
