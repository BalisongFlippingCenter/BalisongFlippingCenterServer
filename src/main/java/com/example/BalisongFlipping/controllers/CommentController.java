package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.commentDtos.CreateCommentDto;
import com.example.BalisongFlipping.dtos.commentDtos.EditCommentDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    @Autowired private CommentService commentService;
    @Autowired private AccountService accountService;

    // -------------------------------------------------------------------------
    // Public — no auth required
    // -------------------------------------------------------------------------

    @GetMapping("/posts/any/{postId}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            return new ResponseEntity<>(commentService.getComments(postId, page, size), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/any/{}/comments -> {}", postId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/posts/any/{postId}/comments/{commentId}/replies")
    public ResponseEntity<?> getReplies(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            return new ResponseEntity<>(commentService.getReplies(commentId, page, size), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/any/{}/comments/{}/replies -> {}", postId, commentId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // Auth required
    // -------------------------------------------------------------------------

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentDto dto
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(
                    commentService.createComment(postId, accountId, dto.content(), dto.parentCommentId()),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            log.error("POST /posts/{}/comments -> {}", postId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> editComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody EditCommentDto dto
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(
                    commentService.editComment(commentId, accountId, dto.content()),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("PUT /posts/{}/comments/{} -> {}", postId, commentId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        try {
            String accountId = accountService.getSelf().id();
            commentService.deleteComment(commentId, accountId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            log.error("DELETE /posts/{}/comments/{} -> {}", postId, commentId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(commentService.likeComment(commentId, accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /posts/{}/comments/{}/like -> {}", postId, commentId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<?> unlikeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(commentService.unlikeComment(commentId, accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("DELETE /posts/{}/comments/{}/like -> {}", postId, commentId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
