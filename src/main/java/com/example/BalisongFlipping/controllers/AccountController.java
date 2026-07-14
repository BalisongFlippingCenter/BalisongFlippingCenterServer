package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.ConfirmEmailChangeDto;
import com.example.BalisongFlipping.dtos.ConfirmPasswordChangeDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdatePreferencesDto;
import com.example.BalisongFlipping.dtos.UpdateSocialLinksDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/accounts")
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String s3Region;

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    // -------------------------------------------------------------------------
    // Public profile reads — no auth required
    // -------------------------------------------------------------------------

    @GetMapping("/any/search")
    public ResponseEntity<?> searchUsers(@RequestParam("q") String query) {
        try {
            return new ResponseEntity<>(accountService.searchUsers(query), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /accounts/any/search?q={} -> {}", query, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/any/{accountId}")
    public ResponseEntity<?> getPublicProfileById(@PathVariable("accountId") String accountId) {
        try {
            return new ResponseEntity<>(accountService.getPublicProfileById(accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /accounts/any/{} -> {}", accountId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/any")
    public ResponseEntity<?> getPublicProfileByHandle(
            @RequestParam("displayName") String displayName,
            @RequestParam("identifierCode") String identifierCode
    ) {
        try {
            return new ResponseEntity<>(accountService.getPublicProfileByHandle(displayName, identifierCode), HttpStatus.OK);
        } catch (Exception e) {
            log.error("GET /accounts/any?displayName={}&identifierCode={} -> {}", displayName, identifierCode, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // Self
    // -------------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            return ResponseEntity.ok(accountService.getSelf());
        } catch (Exception e) {
            log.error("GET /me -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    @PostMapping("/me/change-display-name")
    public ResponseEntity<?> changeDisplayName(@RequestBody String newDisplayName) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.changeDisplayName(accountId, newDisplayName.strip()), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/change-display-name -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/update-bio")
    public ResponseEntity<?> updateBio(@RequestBody String bio) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.updateBio(accountId, bio.strip()), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/update-bio -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping(value = "/me/update-profile-img", consumes = "multipart/form-data")
    public ResponseEntity<?> updateProfileImg(@RequestParam("file") MultipartFile file) {
        try {
            String accountId = accountService.getSelf().id();
            String key = "profile-images/" + accountId + "/" + file.getOriginalFilename();
            s3Service.uploadFile(bucketName, key, file.getSize(), file.getContentType(), file.getInputStream());
            String url = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
            return new ResponseEntity<>(accountService.updateProfileImg(accountId, url), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/update-profile-img -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping(value = "/me/update-banner-img", consumes = "multipart/form-data")
    public ResponseEntity<?> updateBannerImg(@RequestParam("file") MultipartFile file) {
        try {
            String accountId = accountService.getSelf().id();
            String key = "banner-images/" + accountId + "/" + file.getOriginalFilename();
            s3Service.uploadFile(bucketName, key, file.getSize(), file.getContentType(), file.getInputStream());
            String url = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
            return new ResponseEntity<>(accountService.updateBannerImg(accountId, url), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/update-banner-img -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Social links (consolidated)
    // -------------------------------------------------------------------------

    @PostMapping("/me/update-social-links")
    public ResponseEntity<?> updateSocialLinks(@RequestBody UpdateSocialLinksDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.updateSocialLinks(accountId, dto), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/update-social-links -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // App preferences
    // -------------------------------------------------------------------------

    @PostMapping("/me/update-preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody UpdatePreferencesDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.updatePreferences(accountId, dto), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/update-preferences -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Follow / unfollow
    // -------------------------------------------------------------------------

    @GetMapping("/any/{accountId}/following")
    public ResponseEntity<?> getFollowing(@PathVariable("accountId") String accountId) {
        try {
            return ResponseEntity.ok(accountService.getFollowing(accountId));
        } catch (Exception e) {
            log.error("GET /accounts/any/{}/following -> {}", accountId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/any/{accountId}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable("accountId") String accountId) {
        try {
            return ResponseEntity.ok(accountService.getFollowers(accountId));
        } catch (Exception e) {
            log.error("GET /accounts/any/{}/followers -> {}", accountId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/any/{targetId}/follow")
    public ResponseEntity<?> checkIsFollowing(@PathVariable("targetId") String targetId) {
        try {
            String selfId = null;
            try { selfId = accountService.getSelf().id(); } catch (Exception ignored) {}
            boolean following = selfId != null && accountService.isFollowing(selfId, targetId);
            return ResponseEntity.ok(java.util.Map.of("following", following));
        } catch (Exception e) {
            log.error("GET /accounts/any/{}/follow -> {}", targetId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/any/{targetId}/follow")
    public ResponseEntity<?> followAccount(@PathVariable("targetId") String targetId) {
        try {
            String followerId = accountService.getSelf().id();
            return ResponseEntity.ok(accountService.followAccount(followerId, targetId));
        } catch (Exception e) {
            log.error("POST /accounts/any/{}/follow -> {}", targetId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/any/{targetId}/follow")
    public ResponseEntity<?> unfollowAccount(@PathVariable("targetId") String targetId) {
        try {
            String followerId = accountService.getSelf().id();
            return ResponseEntity.ok(accountService.unfollowAccount(followerId, targetId));
        } catch (Exception e) {
            log.error("DELETE /accounts/any/{}/follow -> {}", targetId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Email / password change (2-step via email code)
    // -------------------------------------------------------------------------

    @PostMapping("/me/request-email-change")
    public ResponseEntity<?> requestEmailChange() {
        try {
            String accountId = accountService.getSelf().id();
            accountService.requestEmailChange(accountId);
            return ResponseEntity.ok("Verification code sent to your current email.");
        } catch (Exception e) {
            log.error("POST /me/request-email-change -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/confirm-email-change")
    public ResponseEntity<?> confirmEmailChange(@RequestBody ConfirmEmailChangeDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            return ResponseEntity.ok(accountService.confirmEmailChange(accountId, dto));
        } catch (Exception e) {
            log.error("POST /me/confirm-email-change -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/request-password-change")
    public ResponseEntity<?> requestPasswordChange() {
        try {
            String accountId = accountService.getSelf().id();
            accountService.requestPasswordChange(accountId);
            return ResponseEntity.ok("Verification code sent to your email.");
        } catch (Exception e) {
            log.error("POST /me/request-password-change -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/confirm-password-change")
    public ResponseEntity<?> confirmPasswordChange(@RequestBody ConfirmPasswordChangeDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            accountService.confirmPasswordChange(accountId, dto);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (Exception e) {
            log.error("POST /me/confirm-password-change -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Danger zone
    // -------------------------------------------------------------------------

    @PostMapping("/me/hide-account")
    public ResponseEntity<?> hideAccount() {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.hideAccount(accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/hide-account -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/reset-account")
    public ResponseEntity<?> resetAccount() {
        try {
            String accountId = accountService.getSelf().id();
            return new ResponseEntity<>(accountService.resetAccount(accountId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /me/reset-account -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount() {
        try {
            String accountId = accountService.getSelf().id();
            accountService.deleteAccount(accountId);
            return new ResponseEntity<>("Account deleted.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("DELETE /me -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
