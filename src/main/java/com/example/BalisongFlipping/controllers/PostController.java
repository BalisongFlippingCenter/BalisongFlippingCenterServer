package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.postsDtos.CreatePostRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostResponseDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.UpdatePostDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.modals.posts.PostWrapper;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/posts")
@RestController
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private PostService postService;

    @Autowired
    private AccountService accountService;

    // -------------------------------------------------------------------------
    // Fetch posts — public, no token required
    // -------------------------------------------------------------------------

    @GetMapping("/any/{id}")
    public ResponseEntity<?> getPostById(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(postService.getPostById(id), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/any/{} -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/any")
    public ResponseEntity<?> getPosts(
            @RequestParam(value = "postType", required = false) String postType,
            @RequestParam(value = "accountId", required = false) String accountId,
            @RequestParam(value = "difficultyTag", required = false) String difficultyTag,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "knifeBladeStyle", required = false) String knifeBladeStyle,
            @RequestParam(value = "knifeBladeMaterial", required = false) String knifeBladeMaterial,
            @RequestParam(value = "knifeBladeFinish", required = false) String knifeBladeFinish,
            @RequestParam(value = "knifeHandleMaterial", required = false) String knifeHandleMaterial,
            @RequestParam(value = "knifeHandleConstruction", required = false) String knifeHandleConstruction,
            @RequestParam(value = "knifeHandleFinish", required = false) String knifeHandleFinish,
            @RequestParam(value = "knifePivotSystem", required = false) String knifePivotSystem,
            @RequestParam(value = "knifePinSystem", required = false) String knifePinSystem,
            @RequestParam(value = "knifeLatchType", required = false) String knifeLatchType,
            @RequestParam(value = "knifeType", required = false) String knifeType
    ) {
        try {
            String selfId = null;
            try { selfId = accountService.getSelf().id(); } catch (Exception ignored) {}
            Page<PostResponseDto> result = postService.getPosts(
                    postType, accountId, difficultyTag, search, page, size, selfId,
                    knifeBladeStyle, knifeBladeMaterial, knifeBladeFinish,
                    knifeHandleMaterial, knifeHandleConstruction, knifeHandleFinish,
                    knifePivotSystem, knifePinSystem, knifeLatchType, knifeType
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/any -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Request presigned S3 upload URLs for post media
    // Client PUTs each file directly to S3, then calls /posts/create with the
    // resulting URLs instead of raw files.
    // -------------------------------------------------------------------------

    @PostMapping("/upload-url")
    public ResponseEntity<?> getUploadUrls(@RequestBody PostUploadUrlRequestDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            List<PresignedUploadTargetDto> targets = postService.generateUploadUrls(accountId, dto);
            return new ResponseEntity<>(targets, HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /posts/upload-url -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Create post
    // All logic is handled in PostService based on postType.
    // Media must already be uploaded to S3 via /posts/upload-url — this only
    // persists metadata referencing those URLs.
    // Required for all types:  postType, caption, media
    // Type-specific optionals: description, referenceKnifeId, mode,
    //                          offeringKnifeId, lookingForText,
    //                          tags, difficultyTag, techniqueTags
    // -------------------------------------------------------------------------

    @PostMapping("/create")
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequestDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            PostWrapper post = postService.createPost(accountId, dto);
            return new ResponseEntity<>(post, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("POST /posts/create [{}] -> {}", dto.postType(), e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Liked posts — requires auth
    // -------------------------------------------------------------------------

    @GetMapping("/me/liked")
    public ResponseEntity<?> getLikedPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(postService.getLikedPosts(accountId, page, size), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/me/liked -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Update post — owner only
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable("id") Long id, @RequestBody UpdatePostDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(postService.updatePost(id, accountId, dto), HttpStatus.OK);
        } catch (Exception e) {
            log.error("PATCH /posts/{} -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Delete post — owner only
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable("id") Long id) {
        try {
            String accountId = accountService.getSelf().id();
            postService.deletePost(id, accountId);
            return new ResponseEntity<>(Map.of("message", "Post deleted."), HttpStatus.OK);
        } catch (Exception e) {
            log.error("DELETE /posts/{} -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Like / Unlike — requires auth
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable("id") Long id) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(postService.likePost(id, accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /posts/{}/like -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikePost(@PathVariable("id") Long id) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(postService.unlikePost(id, accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("DELETE /posts/{}/like -> {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
