package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.CollectionDataDto;
import com.example.BalisongFlipping.dtos.CollectionProfileDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdateKnifeDto;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.CollectionService;
import com.example.BalisongFlipping.services.PostService;
import com.example.BalisongFlipping.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequestMapping("/collection")
@RestController
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PostService postService;

    @Autowired
    private S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String s3Region;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Public collection profile reads — no auth required
    // -------------------------------------------------------------------------

    @GetMapping("/any/account/{accountId}")
    public ResponseEntity<?> getCollectionByAccountId(@PathVariable("accountId") String accountId) {
        try {
            PublicProfileDto profile = accountService.getPublicProfileById(accountId);
            CollectionDataDto collection = collectionService.getCollectionByAccountId(accountId);
            if (collection == null) return new ResponseEntity<>("Collection not found.", HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(
                    new CollectionProfileDto(profile.id(), profile.displayName(), profile.identifierCode(), profile.profileImg(), collection),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("GET /collection/any/account/{} -> {}", accountId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/any/handle")
    public ResponseEntity<?> getCollectionByHandle(
            @RequestParam("displayName") String displayName,
            @RequestParam("identifierCode") String identifierCode
    ) {
        try {
            PublicProfileDto profile = accountService.getPublicProfileByHandle(displayName, identifierCode);
            CollectionDataDto collection = collectionService.getCollectionByAccountId(profile.id());
            if (collection == null) return new ResponseEntity<>("Collection not found.", HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(
                    new CollectionProfileDto(profile.id(), profile.displayName(), profile.identifierCode(), profile.profileImg(), collection),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("GET /collection/any/handle?displayName={}&identifierCode={} -> {}", displayName, identifierCode, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/any/{collectionId}/get-posts")
    public ResponseEntity<?> getCollectionPosts(@PathVariable("collectionId") String collectionId) {
        try {
            CollectionDataDto foundCollection = collectionService.getCollection(collectionId);
            return new ResponseEntity<>(postService.getCollectionTimelinePosts(foundCollection.accountId()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @GetMapping(value = "/any/{collectionId}")
    public ResponseEntity<?> getCollectionById(@PathVariable("collectionId") String collectionId) {
        CollectionDataDto foundCollection = collectionService.getCollection(collectionId);
        if (foundCollection == null) {
            return new ResponseEntity<>("Error retrieving collection", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(foundCollection, HttpStatus.OK);
    }

    // -------------------------------------------------------------------------
    // Collection banner
    // -------------------------------------------------------------------------

    @PostMapping(value = "/me/update-banner-img", consumes = "multipart/form-data")
    public ResponseEntity<?> updateBannerImg(@RequestParam("file") MultipartFile file) {
        try {
            String collectionId = accountService.getSelf().collectionId();

            if (!collectionService.checkForCollectionExistance(collectionId)) {
                return new ResponseEntity<>("Collection doesn't exist", HttpStatus.NOT_FOUND);
            }

            String key = "collection-banners/" + collectionId + "/" +
                    (file.getOriginalFilename() != null ? file.getOriginalFilename() : UUID.randomUUID().toString());
            s3Service.uploadFile(bucketName, key, file.getSize(), file.getContentType(), file.getInputStream());
            String url = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;

            return new ResponseEntity<>(collectionService.updateBannerImg(collectionId, url), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /collection/me/update-banner-img -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Featured knife
    // -------------------------------------------------------------------------

    @PostMapping("/me/set-featured-knife/{knifeId}")
    public ResponseEntity<?> setFeaturedKnife(@PathVariable("knifeId") String knifeId) {
        try {
            String collectionId = accountService.getSelf().collectionId();
            return new ResponseEntity<>(collectionService.setFeaturedKnife(collectionId, knifeId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /collection/me/set-featured-knife/{} -> {}", knifeId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/me/clear-featured-knife")
    public ResponseEntity<?> clearFeaturedKnife() {
        try {
            String collectionId = accountService.getSelf().collectionId();
            return new ResponseEntity<>(collectionService.clearFeaturedKnife(collectionId), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /collection/me/clear-featured-knife -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Update knife cover photo
    // -------------------------------------------------------------------------

    @PostMapping(value = "/me/update-knife/{knifeId}/cover-photo", consumes = "multipart/form-data")
    public ResponseEntity<?> updateKnifeCoverPhoto(
            @PathVariable("knifeId") String knifeId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "existingUrl", required = false) String existingUrl) {
        try {
            // Exactly one source must be provided
            boolean hasFile = file != null && !file.isEmpty();
            boolean hasUrl = existingUrl != null && !existingUrl.isBlank();

            if (!hasFile && !hasUrl)
                return new ResponseEntity<>("Either a file or an existing URL must be provided.", HttpStatus.CONFLICT);
            if (hasFile && hasUrl)
                return new ResponseEntity<>("Provide either a file or an existing URL, not both.", HttpStatus.CONFLICT);

            String collectionId = accountService.getSelf().collectionId();

            String coverUrl;
            if (hasFile) {
                // Fetch display name for S3 key path
                String safeDisplayName = collectionService.getKnifeDisplayName(collectionId, knifeId);
                String key = "collection-knives/" + collectionId + "/" + safeDisplayName + "/cover/" +
                        (file.getOriginalFilename() != null ? file.getOriginalFilename() : UUID.randomUUID().toString());
                s3Service.uploadFile(bucketName, key, file.getSize(), file.getContentType(), file.getInputStream());
                coverUrl = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
            } else {
                coverUrl = existingUrl;
            }

            return new ResponseEntity<>(collectionService.updateKnifeCoverPhoto(collectionId, knifeId, coverUrl), HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /collection/me/update-knife/{}/cover-photo -> {}", knifeId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Delete knife
    // -------------------------------------------------------------------------

    @DeleteMapping("/me/remove-knife/{knifeId}")
    public ResponseEntity<?> removeKnife(@PathVariable("knifeId") String knifeId) {
        try {
            String collectionId = accountService.getSelf().collectionId();
            collectionService.deleteKnife(collectionId, knifeId);
            return new ResponseEntity<>("Knife removed from collection.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("DELETE /collection/me/remove-knife/{} -> {}", knifeId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Update knife
    // -------------------------------------------------------------------------

    @PutMapping("/me/update-knife/{knifeId}")
    public ResponseEntity<?> updateKnife(
            @PathVariable("knifeId") String knifeId,
            @RequestBody UpdateKnifeDto dto) {
        try {
            String collectionId = accountService.getSelf().collectionId();
            return new ResponseEntity<>(collectionService.updateKnife(collectionId, knifeId, dto), HttpStatus.OK);
        } catch (Exception e) {
            log.error("PUT /collection/me/update-knife/{} -> {}", knifeId, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Add knife
    // -------------------------------------------------------------------------

    @PostMapping(value = "/me/add-knife", consumes = "multipart/form-data")
    public ResponseEntity<?> addKnifeToCollection(
            // Required
            @RequestParam("displayName") String displayName,
            @RequestParam("knifeMaker") String knifeMaker,
            @RequestParam("baseKnifeModel") String baseKnifeModel,
            @RequestParam("knifeType") String knifeType,
            @RequestParam("aqquiredDate") String aqquiredDate,
            @RequestParam("coverPhoto") MultipartFile coverPhoto,
            // Optional toggles
            @RequestParam(value = "isFavoriteKnife", required = false, defaultValue = "false") String isFavoriteKnife,
            @RequestParam(value = "isFavoriteFlipper", required = false, defaultValue = "false") String isFavoriteFlipper,
            // Optional specs
            @RequestParam(value = "knifeMSRP", required = false, defaultValue = "0") String knifeMSRP,
            @RequestParam(value = "overallLength", required = false, defaultValue = "0") String overallLength,
            @RequestParam(value = "weight", required = false, defaultValue = "0") String weight,
            @RequestParam(value = "pivotSystem", required = false, defaultValue = "unknown") String pivotSystem,
            @RequestParam(value = "latchType", required = false, defaultValue = "unknown") String latchType,
            @RequestParam(value = "pinSystem", required = false, defaultValue = "unknown") String pinSystem,
            @RequestParam(value = "hasModularBalance", required = false, defaultValue = "false") String hasModularBalance,
            @RequestParam(value = "balanceValue", required = false, defaultValue = "") String balanceValue,
            // Optional blade
            @RequestParam(value = "bladeStyle", required = false, defaultValue = "unknown") String bladeStyle,
            @RequestParam(value = "bladeFinish", required = false, defaultValue = "unknown") String bladeFinish,
            @RequestParam(value = "bladeMaterial", required = false, defaultValue = "unknown") String bladeMaterial,
            // Optional handles
            @RequestParam(value = "handleConstruction", required = false, defaultValue = "unknown") String handleConstruction,
            @RequestParam(value = "handleMaterial", required = false, defaultValue = "unknown") String handleMaterial,
            @RequestParam(value = "handleFinish", required = false, defaultValue = "unknown") String handleFinish,
            // Optional scores (default to 5 matching UI defaults)
            @RequestParam(value = "qualityScore", required = false, defaultValue = "5") String qualityScore,
            @RequestParam(value = "flippingScore", required = false, defaultValue = "5") String flippingScore,
            @RequestParam(value = "feelScore", required = false, defaultValue = "5") String feelScore,
            @RequestParam(value = "soundScore", required = false, defaultValue = "5") String soundScore,
            @RequestParam(value = "durabilityScore", required = false, defaultValue = "5") String durabilityScore,
            // Optional gallery (max 10 files)
            @RequestParam(value = "galleryFiles", required = false) MultipartFile[] galleryFiles
    ) {
        try {
            String collectionId = accountService.getSelf().collectionId();

            if (!collectionService.checkForCollectionExistance(collectionId)) {
                return new ResponseEntity<>("Collection doesn't exist", HttpStatus.NOT_FOUND);
            }

            if (!collectionService.validateNewKnifeInfo(displayName, knifeMaker, baseKnifeModel, knifeType, aqquiredDate, coverPhoto)) {
                return new ResponseEntity<>("Invalid info passed.", HttpStatus.CONFLICT);
            }

            if (collectionService.checkForDuplicateDisplayName(displayName, collectionId)) {
                return new ResponseEntity<>("Duplicate display name.", HttpStatus.CONFLICT);
            }

            if (galleryFiles != null && galleryFiles.length > 10) {
                return new ResponseEntity<>("Gallery cannot exceed 10 files.", HttpStatus.CONFLICT);
            }

            // Upload cover photo to S3
            String safeDisplayName = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String coverKey = "collection-knives/" + collectionId + "/" + safeDisplayName + "/cover/" +
                    (coverPhoto.getOriginalFilename() != null ? coverPhoto.getOriginalFilename() : UUID.randomUUID().toString());
            s3Service.uploadFile(bucketName, coverKey, coverPhoto.getSize(), coverPhoto.getContentType(), coverPhoto.getInputStream());
            String coverUrl = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + coverKey;

            // Upload gallery files to S3
            List<String> galleryUrls = new ArrayList<>();
            if (galleryFiles != null) {
                for (MultipartFile galleryFile : galleryFiles) {
                    String galleryKey = "collection-knives/" + collectionId + "/" + safeDisplayName + "/gallery/" +
                            (galleryFile.getOriginalFilename() != null ? galleryFile.getOriginalFilename() : UUID.randomUUID().toString());
                    s3Service.uploadFile(bucketName, galleryKey, galleryFile.getSize(), galleryFile.getContentType(), galleryFile.getInputStream());
                    galleryUrls.add("https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + galleryKey);
                }
            }

            // Parse scores
            double qualityScoreVal = parseScore(qualityScore);
            double flippingScoreVal = parseScore(flippingScore);
            double feelScoreVal = parseScore(feelScore);
            double soundScoreVal = parseScore(soundScore);
            double durabilityScoreVal = parseScore(durabilityScore);

            CollectionKnife newKnife = collectionService.addNewKnife(
                    collectionId, displayName, knifeMaker, baseKnifeModel, knifeType,
                    aqquiredDate, isFavoriteKnife, isFavoriteFlipper, coverUrl,
                    knifeMSRP, overallLength, weight, pivotSystem, latchType, pinSystem,
                    hasModularBalance, balanceValue, bladeStyle, bladeFinish, bladeMaterial,
                    handleConstruction, handleMaterial, handleFinish,
                    qualityScoreVal, flippingScoreVal, feelScoreVal, soundScoreVal, durabilityScoreVal,
                    galleryUrls
            );

            return new ResponseEntity<>(newKnife, HttpStatus.OK);
        } catch (Exception e) {
            log.error("POST /collection/me/add-knife -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double parseScore(String value) {
        try { return Double.parseDouble(value); } catch (Exception e) { return 0.0; }
    }
}
