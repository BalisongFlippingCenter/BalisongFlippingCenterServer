package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.postsDtos.PostResponseDto;
import com.example.BalisongFlipping.modals.posts.PostWrapper;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Page<PostResponseDto> result = postService.getPosts(postType, accountId, difficultyTag, page, size);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /posts/any -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Create post
    // All logic is handled in PostService based on postType.
    // Required for all types:  postType, caption, mediaFiles
    // Type-specific optionals: description, referenceKnifeId, mode,
    //                          offeringKnifeId, lookingForText,
    //                          tags, difficultyTag, techniqueTags
    // -------------------------------------------------------------------------

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestParam("postType") String postType,
            @RequestParam("caption") String caption,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "referenceKnifeId", required = false) String referenceKnifeId,
            @RequestParam(value = "mediaFiles", required = false) MultipartFile[] mediaFiles,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "offeringKnifeId", required = false) String offeringKnifeId,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "lookingForText", required = false) String lookingForText,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "difficultyTag", required = false) String difficultyTag,
            @RequestParam(value = "techniqueTags", required = false) String[] techniqueTags
    ) {
        try {
            String accountId = accountService.getSelf().id();

            PostWrapper post = postService.createPost(
                    accountId,
                    postType,
                    caption,
                    description,
                    referenceKnifeId,
                    mediaFiles,
                    mode,
                    offeringKnifeId,
                    price,
                    lookingForText,
                    tags,
                    difficultyTag,
                    techniqueTags
            );

            return new ResponseEntity<>(post, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("POST /posts/create [{}] -> {}", postType, e.getMessage());
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
