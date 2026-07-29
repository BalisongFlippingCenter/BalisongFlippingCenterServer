package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.postsDtos.CollectionTimelineDto;
import com.example.BalisongFlipping.dtos.postsDtos.CreatePostRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.FileMetadataItem;
import com.example.BalisongFlipping.dtos.postsDtos.PostMediaInputDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.UpdatePostDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.dtos.postsDtos.PostAuthorDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostKnifeDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostResponseDto;
import com.example.BalisongFlipping.enums.posts.BuySellMode;
import com.example.BalisongFlipping.enums.posts.tags.DifficultyTag;
import com.example.BalisongFlipping.enums.posts.tags.GenericPostTag;
import com.example.BalisongFlipping.enums.posts.tags.TechniqueTag;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.modals.collectionKnives.GalleryFile;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.modals.posts.*;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.repositories.PostLikeRepository;
import com.example.BalisongFlipping.repositories.PostsRepository;
import com.example.BalisongFlipping.enums.knives.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CollectionKnifeRepository collectionKnifeRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String s3Region;

    // -------------------------------------------------------------------------
    // Legacy stubs (used by CollectionController timeline endpoint)
    // -------------------------------------------------------------------------

    public CollectionTimelinePost createAddKnifeCollectionTimelinePost(String accountId, CollectionKnife newKnife, List<String> fileIds) throws Exception {
        return null;
    }

    public List<CollectionTimelineDto> getCollectionTimelinePosts(String accountId) throws Exception {
        return new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Fetch posts — public, paginated, optional filters
    // -------------------------------------------------------------------------

    public PostResponseDto getPostById(Long id) throws Exception {
        PostWrapper post = postsRepository.findById(id)
                .orElseThrow(() -> new Exception("Post not found."));
        return buildPostResponse(post);
    }

    public Page<PostResponseDto> getPosts(
            String postType, String accountId, String difficultyTag, String search,
            int page, int size, String selfId,
            String knifeBladeStyle, String knifeBladeMaterial, String knifeBladeFinish,
            String knifeHandleMaterial, String knifeHandleConstruction, String knifeHandleFinish,
            String knifePivotSystem, String knifePinSystem, String knifeLatchType,
            String knifeType
    ) throws Exception {
        // Resolve before the lambda — checked exceptions can't be thrown from inside Specification.toPredicate
        final Class<? extends PostWrapper> typeClass = (postType != null && !postType.isBlank())
                ? resolvePostTypeClass(postType)
                : null;

        final DifficultyTag parsedDifficulty = (difficultyTag != null && !difficultyTag.isBlank())
                ? DifficultyTag.valueOf(difficultyTag.toUpperCase().trim())
                : null;

        final String searchTerm = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        final BladeStyle parsedBladeStyle = parseKnifeEnum(BladeStyle.class, knifeBladeStyle);
        final BladeMaterial parsedBladeMaterial = parseKnifeEnum(BladeMaterial.class, knifeBladeMaterial);
        final BladeFinish parsedBladeFinish = parseKnifeEnum(BladeFinish.class, knifeBladeFinish);
        final HandleMaterial parsedHandleMaterial = parseKnifeEnum(HandleMaterial.class, knifeHandleMaterial);
        final HandleConstruction parsedHandleConstruction = parseKnifeEnum(HandleConstruction.class, knifeHandleConstruction);
        final HandleFinish parsedHandleFinish = parseKnifeEnum(HandleFinish.class, knifeHandleFinish);
        final PivotSystem parsedPivotSystem = parseKnifeEnum(PivotSystem.class, knifePivotSystem);
        final PinSystem parsedPinSystem = parseKnifeEnum(PinSystem.class, knifePinSystem);
        final LatchType parsedLatchType = parseKnifeEnum(LatchType.class, knifeLatchType);
        final KnifeType parsedKnifeType = parseKnifeEnum(KnifeType.class, knifeType);

        final boolean knifeFiltersPresent = parsedBladeStyle != null || parsedBladeMaterial != null
                || parsedBladeFinish != null || parsedHandleMaterial != null
                || parsedHandleConstruction != null || parsedHandleFinish != null
                || parsedPivotSystem != null || parsedPinSystem != null
                || parsedLatchType != null || parsedKnifeType != null;

        Specification<PostWrapper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (accountId != null && !accountId.isBlank()) {
                predicates.add(cb.equal(root.get("accountId"), accountId));
            }

            if (typeClass != null) {
                predicates.add(cb.equal(root.type(), typeClass));
            } else if (searchTerm != null) {
                // Search is only meaningful on COMBO and TRICK_TUTORIAL — restrict automatically
                predicates.add(cb.or(
                        cb.equal(root.type(), ComboPost.class),
                        cb.equal(root.type(), TrickTutorialPost.class)
                ));
            }

            if (parsedDifficulty != null) {
                Predicate comboMatch = cb.equal(
                        cb.treat(root, ComboPost.class).get("difficultyTag"), parsedDifficulty);
                Predicate tutorialMatch = cb.equal(
                        cb.treat(root, TrickTutorialPost.class).get("difficultyTag"), parsedDifficulty);
                predicates.add(cb.or(comboMatch, tutorialMatch));
            }

            if (searchTerm != null) {
                Predicate captionMatch = cb.like(cb.lower(root.get("caption")), searchTerm);
                Predicate descriptionMatch = cb.like(cb.lower(root.get("description")), searchTerm);
                predicates.add(cb.or(captionMatch, descriptionMatch));
            }

            if (knifeFiltersPresent) {
                Subquery<Long> knifeSub = query.subquery(Long.class);
                Root<CollectionKnife> kr = knifeSub.from(CollectionKnife.class);
                knifeSub.select(kr.get("id"));
                List<Predicate> kp = new ArrayList<>();
                if (parsedBladeStyle != null) kp.add(cb.equal(kr.get("bladeStyle"), parsedBladeStyle));
                if (parsedBladeMaterial != null) kp.add(cb.equal(kr.get("bladeMaterial"), parsedBladeMaterial));
                if (parsedBladeFinish != null) kp.add(cb.equal(kr.get("bladeFinish"), parsedBladeFinish));
                if (parsedHandleMaterial != null) kp.add(cb.equal(kr.get("handleMaterial"), parsedHandleMaterial));
                if (parsedHandleConstruction != null) kp.add(cb.equal(kr.get("handleConstruction"), parsedHandleConstruction));
                if (parsedHandleFinish != null) kp.add(cb.equal(kr.get("handleFinish"), parsedHandleFinish));
                if (parsedPivotSystem != null) kp.add(cb.equal(kr.get("pivotSystem"), parsedPivotSystem));
                if (parsedPinSystem != null) kp.add(cb.equal(kr.get("pinSystem"), parsedPinSystem));
                if (parsedLatchType != null) kp.add(cb.equal(kr.get("latchType"), parsedLatchType));
                if (parsedKnifeType != null) kp.add(cb.equal(kr.get("knifeType"), parsedKnifeType));
                knifeSub.where(cb.and(kp.toArray(new Predicate[0])));

                Predicate buySellMatch = cb.and(
                        cb.equal(root.type(), BuySellPost.class),
                        cb.treat(root, BuySellPost.class).get("offeringKnifeId").in(knifeSub)
                );
                Predicate tradeMatch = cb.and(
                        cb.equal(root.type(), TradePost.class),
                        cb.treat(root, TradePost.class).get("offeringKnifeId").in(knifeSub)
                );
                predicates.add(cb.or(buySellMatch, tradeMatch));
            }

            // Show private posts only when the requester is viewing their own profile
            boolean viewingOwnPosts = selfId != null && selfId.equals(accountId);
            if (!viewingOwnPosts) {
                predicates.add(cb.isFalse(root.get("isPrivate")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationDate"));
        return postsRepository.findAll(spec, pageable).map(this::buildPostResponse);
    }

    private PostResponseDto buildPostResponse(PostWrapper post) {
        PostAuthorDto author = null;
        try {
            if (post.getAccountId() != null) {
                Account account = accountRepository.findById(Long.parseLong(post.getAccountId())).orElse(null);
                if (account instanceof User user) {
                    author = new PostAuthorDto(
                            post.getAccountId(),
                            user.getDisplayName(),
                            user.getIdentifierCode(),
                            user.getProfileImg()
                    );
                }
            }
        } catch (Exception ignored) {}

        PostKnifeDto offeringKnife = null;
        Long offeringKnifeId = null;
        if (post instanceof TradePost tp) {
            offeringKnifeId = tp.getOfferingKnifeId();
        } else if (post instanceof BuySellPost bsp) {
            offeringKnifeId = bsp.getOfferingKnifeId();
        }
        if (offeringKnifeId != null) {
            CollectionKnife knife = collectionKnifeRepository.findById(offeringKnifeId).orElse(null);
            if (knife != null) {
                offeringKnife = PostKnifeDto.from(knife);
            }
        }

        // Resolve per-media referenceKnife objects
        if (post.getMediaFiles() != null) {
            for (PostMedia pm : post.getMediaFiles()) {
                if (pm.getReferenceKnifeId() != null) {
                    CollectionKnife k = collectionKnifeRepository.findById(pm.getReferenceKnifeId()).orElse(null);
                    if (k != null) {
                        pm.setReferenceKnife(PostKnifeDto.from(k));
                    }
                }
            }
        }

        PostKnifeDto referenceKnife = null;
        Long referenceKnifeId = post.getReferenceKnifeId();
        if (referenceKnifeId != null) {
            CollectionKnife knife = collectionKnifeRepository.findById(referenceKnifeId).orElse(null);
            if (knife != null) {
                referenceKnife = PostKnifeDto.from(knife);
            }
        }

        return new PostResponseDto(post, author, offeringKnife, referenceKnife);
    }

    // -------------------------------------------------------------------------
    // Liked posts
    // -------------------------------------------------------------------------

    public Page<PostResponseDto> getLikedPosts(String accountId, int page, int size) throws Exception {
        User user = (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        Set<Long> likedIds = user.getLikedPostIds();
        if (likedIds == null || likedIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty();
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationDate"));
        return postsRepository.findByIdIn(likedIds, pageable).map(this::buildPostResponse);
    }

    // -------------------------------------------------------------------------
    // Like / Unlike
    // -------------------------------------------------------------------------

    @Transactional
    public PostResponseDto likePost(Long postId, String accountId) throws Exception {
        PostWrapper post = postsRepository.findById(postId)
                .orElseThrow(() -> new Exception("Post not found."));

        PostLikeId likeId = new PostLikeId(Long.parseLong(accountId), postId);
        if (postLikeRepository.existsById(likeId)) throw new Exception("Post already liked.");

        postLikeRepository.save(new PostLike(likeId));
        post.setLikeCount(post.getLikeCount() + 1);
        postsRepository.save(post);

        User user = (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        user.getLikedPostIds().add(postId);
        accountRepository.save(user);

        if (post.getAccountId() != null) {
            notificationService.send(Long.parseLong(post.getAccountId()), Long.parseLong(accountId),
                    NotificationType.POST_LIKED, TargetType.POST, postId);
        }

        return buildPostResponse(post);
    }

    @Transactional
    public PostResponseDto unlikePost(Long postId, String accountId) throws Exception {
        PostWrapper post = postsRepository.findById(postId)
                .orElseThrow(() -> new Exception("Post not found."));

        PostLikeId likeId = new PostLikeId(Long.parseLong(accountId), postId);
        if (!postLikeRepository.existsById(likeId)) throw new Exception("Post not liked.");

        postLikeRepository.deleteById(likeId);
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postsRepository.save(post);

        User user = (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        user.getLikedPostIds().remove(postId);
        accountRepository.save(user);

        return buildPostResponse(post);
    }

    private Class<? extends PostWrapper> resolvePostTypeClass(String postType) throws Exception {
        switch (postType.toUpperCase().trim()) {
            case "GENERIC":        return GenericPost.class;
            case "BUY_SELL":       return BuySellPost.class;
            case "TRADE":          return TradePost.class;
            case "TRICK_TUTORIAL": return TrickTutorialPost.class;
            case "COMBO":          return ComboPost.class;
            default: throw new Exception("Unknown postType: " + postType);
        }
    }

    // -------------------------------------------------------------------------
    // Presigned upload URLs — client PUTs files directly to S3
    // -------------------------------------------------------------------------

    public List<PresignedUploadTargetDto> generateUploadUrls(String accountId, PostUploadUrlRequestDto dto) throws Exception {
        if (dto.postType() == null || dto.postType().isBlank()) throw new Exception("postType is required.");
        if (dto.files() == null || dto.files().isEmpty()) throw new Exception("At least one file is required.");
        if (dto.files().size() > 10) throw new Exception("Cannot request more than 10 upload URLs at once.");

        String postUuid = UUID.randomUUID().toString();
        List<PresignedUploadTargetDto> targets = new ArrayList<>();
        for (FileUploadRequestItem file : dto.files()) {
            String filename = (file.filename() != null && !file.filename().isBlank())
                    ? file.filename()
                    : UUID.randomUUID().toString();
            String key = "posts/" + accountId + "/" + dto.postType().toLowerCase() + "/" + postUuid + "/" + filename;
            String uploadUrl = s3Service.generatePresignedUploadUrl(bucketName, key, file.contentType(), Duration.ofMinutes(10));
            String publicUrl = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
            boolean isVideo = file.contentType() != null && file.contentType().startsWith("video/");
            targets.add(new PresignedUploadTargetDto(key, uploadUrl, publicUrl, isVideo));
        }
        return targets;
    }

    // -------------------------------------------------------------------------
    // Create post — unified entry point branching by postType
    // -------------------------------------------------------------------------

    public PostWrapper createPost(String accountId, CreatePostRequestDto dto) throws Exception {
        if (dto.postType() == null || dto.postType().isBlank()) {
            throw new Exception("postType is required.");
        }
        PostWrapper created;
        switch (dto.postType().toUpperCase().trim()) {
            case "GENERIC":
                created = createGenericPost(accountId, dto.caption(), dto.media(), dto.tags()); break;
            case "BUY_SELL":
                created = createBuySellPost(accountId, dto.caption(), dto.description(), dto.media(), dto.mode(), dto.offeringKnifeId(), dto.price()); break;
            case "TRADE":
                created = createTradePost(accountId, dto.caption(), dto.description(), dto.referenceKnifeId(), dto.media(), dto.offeringKnifeId(), dto.lookingForText()); break;
            case "TRICK_TUTORIAL":
                created = createTrickTutorialPost(accountId, dto.caption(), dto.description(), dto.referenceKnifeId(), dto.media(), dto.difficultyTag(), dto.techniqueTags()); break;
            case "COMBO":
                created = createComboPost(accountId, dto.caption(), dto.description(), dto.referenceKnifeId(), dto.media(), dto.difficultyTag(), dto.techniqueTags()); break;
            default:
                throw new Exception("Unknown postType: " + dto.postType());
        }
        accountService.incrementPostCount(accountId);
        return created;
    }

    // -------------------------------------------------------------------------
    // Generic post
    // -------------------------------------------------------------------------

    @Transactional
    private PostWrapper createGenericPost(
            String accountId,
            String caption,
            List<PostMediaInputDto> mediaInput,
            List<String> tags
    ) throws Exception {
        List<PostMedia> media = resolveMediaList(accountId, mediaInput, "GENERIC", 1, 10);

        List<GenericPostTag> parsedTags = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                try {
                    parsedTags.add(GenericPostTag.valueOf(tag.toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    throw new Exception("Invalid generic tag: " + tag);
                }
            }
        }

        GenericPost post = new GenericPost();
        post.setAccountId(accountId);
        post.setCaption(caption);
        post.setCreationDate(new Date());
        post.setMediaFiles(media);
        post.setTags(parsedTags);
        PostWrapper saved = postsRepository.save(post);

        // Add uploaded files to referenced knife galleries
        for (PostMedia pm : media) {
            if (pm.getReferenceKnifeId() == null) continue;
            addFilesToKnifeGallery(accountId, pm.getReferenceKnifeId(), List.of(pm.getUrl()), saved.getId().toString());
        }

        return saved;
    }

    // -------------------------------------------------------------------------
    // Buy/Sell post
    // -------------------------------------------------------------------------

    private PostWrapper createBuySellPost(
            String accountId,
            String caption,
            String description,
            List<PostMediaInputDto> mediaInput,
            String mode,
            String offeringKnifeId,
            String price
    ) throws Exception {
        if (caption == null || caption.isBlank()) throw new Exception("caption is required.");
        if (mode == null || mode.isBlank()) throw new Exception("mode is required for buy/sell posts.");

        BuySellMode parsedMode;
        try {
            parsedMode = BuySellMode.valueOf(mode.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid mode: " + mode + ". Must be BUYING or SELLING.");
        }

        List<PostMedia> media;

        if (parsedMode == BuySellMode.BUYING) {
            // Buying: exactly 1 image (no videos)
            media = resolveMediaList(accountId, mediaInput, "BUY_SELL", 1, 1);
            if (media.get(0).isVideo()) {
                throw new Exception("Buying posts require an image, not a video.");
            }
        } else {
            // Selling: offeringKnifeId required, validate ownership, up to 10 media, per-file descriptions
            if (offeringKnifeId == null || offeringKnifeId.isBlank()) {
                throw new Exception("offeringKnifeId is required when selling.");
            }
            Long knifeIdLong = Long.parseLong(offeringKnifeId);
            validateKnifeOwnership(accountId, knifeIdLong);

            media = resolveMediaList(accountId, mediaInput, "BUY_SELL", 1, 10);
        }

        BuySellPost post = new BuySellPost();
        populateBase(post, accountId, caption, description, null, media);
        post.setMode(parsedMode);
        if (parsedMode == BuySellMode.SELLING) {
            post.setOfferingKnifeId(Long.parseLong(offeringKnifeId));
        }
        if (price != null && !price.isBlank()) {
            try { post.setPrice(new BigDecimal(price)); } catch (NumberFormatException ignored) {}
        }
        PostWrapper saved = postsRepository.save(post);

        // Add all selling photos to the offering knife's gallery
        if (parsedMode == BuySellMode.SELLING) {
            List<String> urls = media.stream().map(PostMedia::getUrl).toList();
            addFilesToKnifeGallery(accountId, Long.parseLong(offeringKnifeId), urls, saved.getId().toString());
        }

        return saved;
    }

    // -------------------------------------------------------------------------
    // Trade post
    // -------------------------------------------------------------------------

    private PostWrapper createTradePost(
            String accountId,
            String caption,
            String description,
            String referenceKnifeId,
            List<PostMediaInputDto> mediaInput,
            String offeringKnifeId,
            String lookingForText
    ) throws Exception {
        if (caption == null || caption.isBlank()) throw new Exception("caption is required.");
        if (offeringKnifeId == null || offeringKnifeId.isBlank()) throw new Exception("offeringKnifeId is required for trade posts.");
        if (lookingForText == null || lookingForText.isBlank()) throw new Exception("lookingForText is required for trade posts.");

        Long knifeIdLong = Long.parseLong(offeringKnifeId);
        validateKnifeOwnership(accountId, knifeIdLong);

        // Exactly 1 non-video file for the "looking for" photo
        List<PostMedia> media = resolveMediaList(accountId, mediaInput, "TRADE", 1, 1);
        if (media.get(0).isVideo()) {
            throw new Exception("Trade posts require an image for the looking-for photo, not a video.");
        }

        TradePost post = new TradePost();
        populateBase(post, accountId, caption, description, referenceKnifeId, media);
        post.setOfferingKnifeId(knifeIdLong);
        post.setLookingForText(lookingForText);
        return postsRepository.save(post);
    }

    // -------------------------------------------------------------------------
    // Trick tutorial post
    // -------------------------------------------------------------------------

    private PostWrapper createTrickTutorialPost(
            String accountId,
            String caption,
            String description,
            String referenceKnifeId,
            List<PostMediaInputDto> mediaInput,
            String difficultyTag,
            List<String> techniqueTags
    ) throws Exception {
        if (caption == null || caption.isBlank()) throw new Exception("caption (trick name) is required.");
        if (difficultyTag == null || difficultyTag.isBlank()) throw new Exception("difficultyTag is required for trick tutorial posts.");

        DifficultyTag parsedDifficulty;
        try {
            parsedDifficulty = DifficultyTag.valueOf(difficultyTag.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid difficultyTag: " + difficultyTag);
        }

        // Exactly 1 video file
        List<PostMedia> media = resolveMediaList(accountId, mediaInput, "TRICK_TUTORIAL", 1, 1);
        if (!media.get(0).isVideo()) {
            throw new Exception("Trick tutorial posts require a video file.");
        }

        List<TechniqueTag> parsedTechniqueTags = parseTechniqueTags(techniqueTags, 2);

        TrickTutorialPost post = new TrickTutorialPost();
        populateBase(post, accountId, caption, description, referenceKnifeId, media);
        post.setDifficultyTag(parsedDifficulty);
        post.setTechniqueTags(parsedTechniqueTags);
        PostWrapper saved = postsRepository.save(post);

        if (referenceKnifeId != null && !referenceKnifeId.isBlank()) {
            addFilesToKnifeGallery(accountId, Long.parseLong(referenceKnifeId),
                    List.of(media.get(0).getUrl()), saved.getId().toString());
        }

        return saved;
    }

    // -------------------------------------------------------------------------
    // Combo post
    // -------------------------------------------------------------------------

    private PostWrapper createComboPost(
            String accountId,
            String caption,
            String description,
            String referenceKnifeId,
            List<PostMediaInputDto> mediaInput,
            String difficultyTag,
            List<String> techniqueTags
    ) throws Exception {
        if (difficultyTag == null || difficultyTag.isBlank()) throw new Exception("difficultyTag is required for combo posts.");

        DifficultyTag parsedDifficulty;
        try {
            parsedDifficulty = DifficultyTag.valueOf(difficultyTag.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid difficultyTag: " + difficultyTag);
        }

        // Exactly 1 video file
        List<PostMedia> media = resolveMediaList(accountId, mediaInput, "COMBO", 1, 1);
        if (!media.get(0).isVideo()) {
            throw new Exception("Combo posts require a video file.");
        }

        List<TechniqueTag> parsedTechniqueTags = parseTechniqueTags(techniqueTags, 5);

        ComboPost post = new ComboPost();
        populateBase(post, accountId, caption, description, referenceKnifeId, media);
        post.setDifficultyTag(parsedDifficulty);
        post.setTechniqueTags(parsedTechniqueTags);
        PostWrapper saved = postsRepository.save(post);

        if (referenceKnifeId != null && !referenceKnifeId.isBlank()) {
            addFilesToKnifeGallery(accountId, Long.parseLong(referenceKnifeId),
                    List.of(media.get(0).getUrl()), saved.getId().toString());
        }

        return saved;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Update post
    // -------------------------------------------------------------------------

    @Transactional
    public PostResponseDto updatePost(Long postId, String accountId, UpdatePostDto dto) throws Exception {
        PostWrapper post = postsRepository.findById(postId)
                .orElseThrow(() -> new Exception("Post not found."));
        if (!accountId.equals(post.getAccountId()))
            throw new Exception("You do not own this post.");

        if (dto.caption() != null && !dto.caption().isBlank())
            post.setCaption(dto.caption());

        if (dto.isPrivate() != null)
            post.setPrivate(dto.isPrivate());

        String pid = postId.toString();

        if (post instanceof GenericPost) {
            List<PostMedia> media = post.getMediaFiles();
            if (media != null && dto.fileMetadata() != null && !dto.fileMetadata().isBlank()) {
                List<FileMetadataItem> metaList = parseFileMetadata(dto.fileMetadata(), media.size());
                for (int i = 0; i < media.size(); i++) {
                    PostMedia pm = media.get(i);
                    FileMetadataItem meta = metaList.get(i);
                    if (meta == null) continue;

                    if (meta.description() != null) pm.setDescription(meta.description());

                    Long oldKnifeId = pm.getReferenceKnifeId();
                    Long newKnifeId = null;
                    if (meta.referenceKnifeId() != null && !meta.referenceKnifeId().isBlank()) {
                        try { newKnifeId = Long.parseLong(meta.referenceKnifeId()); } catch (NumberFormatException ignored) {}
                    }

                    if (!java.util.Objects.equals(oldKnifeId, newKnifeId)) {
                        if (oldKnifeId != null) removeFilesFromKnifeGallery(oldKnifeId, List.of(pm.getUrl()), pid);
                        if (newKnifeId != null) addFilesToKnifeGallery(accountId, newKnifeId, List.of(pm.getUrl()), pid);
                        pm.setReferenceKnifeId(newKnifeId);
                    }
                }
            }
        } else {
            if (dto.description() != null) post.setDescription(dto.description());

            Long oldKnifeId = post.getReferenceKnifeId();
            Long newKnifeId = null;
            if (dto.referenceKnifeId() != null && !dto.referenceKnifeId().isBlank()) {
                try { newKnifeId = Long.parseLong(dto.referenceKnifeId()); } catch (NumberFormatException ignored) {}
            }

            if (!java.util.Objects.equals(oldKnifeId, newKnifeId)) {
                List<String> fileUrls = post.getMediaFiles() == null ? List.of()
                        : post.getMediaFiles().stream().map(PostMedia::getUrl).toList();
                if (oldKnifeId != null) removeFilesFromKnifeGallery(oldKnifeId, fileUrls, pid);
                if (newKnifeId != null) addFilesToKnifeGallery(accountId, newKnifeId, fileUrls, pid);
                post.setReferenceKnifeId(newKnifeId);
            }
        }

        return buildPostResponse(postsRepository.save(post));
    }

    // -------------------------------------------------------------------------
    // Delete post
    // -------------------------------------------------------------------------

    @Transactional
    public void deletePost(Long postId, String accountId) throws Exception {
        PostWrapper post = postsRepository.findById(postId)
                .orElseThrow(() -> new Exception("Post not found."));
        if (!accountId.equals(post.getAccountId()))
            throw new Exception("You do not own this post.");

        String pid = postId.toString();

        // Remove files from knife galleries
        if (post instanceof GenericPost && post.getMediaFiles() != null) {
            for (PostMedia pm : post.getMediaFiles()) {
                if (pm.getReferenceKnifeId() != null)
                    removeFilesFromKnifeGallery(pm.getReferenceKnifeId(), List.of(pm.getUrl()), pid);
            }
        } else if ((post instanceof TrickTutorialPost || post instanceof ComboPost || post instanceof TradePost)
                && post.getReferenceKnifeId() != null && post.getMediaFiles() != null) {
            List<String> urls = post.getMediaFiles().stream().map(PostMedia::getUrl).toList();
            removeFilesFromKnifeGallery(post.getReferenceKnifeId(), urls, pid);
        } else if (post instanceof BuySellPost bsp && bsp.getMode() == BuySellMode.SELLING
                && bsp.getOfferingKnifeId() != null && post.getMediaFiles() != null) {
            List<String> urls = post.getMediaFiles().stream().map(PostMedia::getUrl).toList();
            removeFilesFromKnifeGallery(bsp.getOfferingKnifeId(), urls, pid);
        }

        postsRepository.delete(post);
        accountService.decrementPostCount(accountId);
    }

    private void removeFilesFromKnifeGallery(Long knifeId, List<String> fileUrls, String postId) {
        CollectionKnife knife = collectionKnifeRepository.findById(knifeId).orElse(null);
        if (knife == null || knife.getGalleryFiles() == null) return;
        Set<String> urlSet = new HashSet<>(fileUrls);
        List<GalleryFile> updated = knife.getGalleryFiles().stream()
                .filter(gf -> !(urlSet.contains(gf.getFileId()) && postId.equals(gf.getPostId())))
                .collect(Collectors.toList());
        knife.setGalleryFiles(updated);
        collectionKnifeRepository.save(knife);
    }

    private void addFilesToKnifeGallery(String accountId, Long knifeId, List<String> fileUrls, String postId) throws Exception {
        validateKnifeOwnership(accountId, knifeId);
        CollectionKnife knife = collectionKnifeRepository.findById(knifeId)
                .orElseThrow(() -> new Exception("Knife not found."));
        List<GalleryFile> gallery = knife.getGalleryFiles() != null
                ? new ArrayList<>(knife.getGalleryFiles())
                : new ArrayList<>();
        for (String url : fileUrls) {
            gallery.add(new GalleryFile(url, postId));
        }
        knife.setGalleryFiles(gallery);
        collectionKnifeRepository.save(knife);
    }

    private void populateBase(
            PostWrapper post,
            String accountId,
            String caption,
            String description,
            String referenceKnifeId,
            List<PostMedia> media
    ) {
        post.setAccountId(accountId);
        post.setCaption(caption);
        post.setDescription(description);
        post.setCreationDate(new Date());
        post.setMediaFiles(media);
        if (referenceKnifeId != null && !referenceKnifeId.isBlank()) {
            try {
                post.setReferenceKnifeId(Long.parseLong(referenceKnifeId));
            } catch (NumberFormatException ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // Resolve client-supplied media URLs (already uploaded to S3 via
    // /posts/upload-url) into PostMedia, verifying ownership and existence.
    // -------------------------------------------------------------------------

    private static final List<String> VIDEO_EXTENSIONS = List.of(".mp4", ".mov", ".webm", ".m4v", ".avi", ".mkv");

    private List<PostMedia> resolveMediaList(
            String accountId,
            List<PostMediaInputDto> mediaInput,
            String postType,
            int minCount,
            int maxCount
    ) throws Exception {
        List<PostMediaInputDto> input = mediaInput != null ? mediaInput : List.of();
        if (input.size() < minCount) throw new Exception(postType + " posts require at least " + minCount + " media file(s).");
        if (input.size() > maxCount) throw new Exception(postType + " posts may not exceed " + maxCount + " media files.");

        List<PostMedia> result = new ArrayList<>();
        for (PostMediaInputDto item : input) {
            String key = validateOwnedUploadedKey(accountId, item.url());
            boolean isVideo = VIDEO_EXTENSIONS.stream().anyMatch(key.toLowerCase()::endsWith);

            PostMedia pm = new PostMedia(item.url(), isVideo);
            if (item.description() != null && !item.description().isBlank()) pm.setDescription(item.description());
            if (item.referenceKnifeId() != null && !item.referenceKnifeId().isBlank()) {
                try { pm.setReferenceKnifeId(Long.parseLong(item.referenceKnifeId())); }
                catch (NumberFormatException ignored) {}
            }
            result.add(pm);
        }
        return result;
    }

    private String validateOwnedUploadedKey(String accountId, String url) throws Exception {
        String prefix = "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/";
        if (url == null || !url.startsWith(prefix)) throw new Exception("Invalid media URL.");

        String key = url.substring(prefix.length());
        if (!key.startsWith("posts/" + accountId + "/")) throw new Exception("You do not own this uploaded file.");
        if (!s3Service.doesObjectExist(bucketName, key)) throw new Exception("Uploaded file not found — it may have failed to upload.");
        return key;
    }

    private List<FileMetadataItem> parseFileMetadata(String fileMetadata, int expectedCount) throws Exception {
        List<FileMetadataItem> result = new ArrayList<>();
        if (fileMetadata == null || fileMetadata.isBlank()) {
            for (int i = 0; i < expectedCount; i++) result.add(null);
            return result;
        }
        try {
            result = objectMapper.readValue(fileMetadata, new TypeReference<List<FileMetadataItem>>() {});
        } catch (JsonProcessingException e) {
            throw new Exception("Invalid fileMetadata JSON: " + e.getMessage());
        }
        if (result.size() != expectedCount) {
            throw new Exception("fileMetadata length (" + result.size() + ") must match file count (" + expectedCount + ").");
        }
        return result;
    }

    private void validateKnifeOwnership(String accountId, Long knifeId) throws Exception {
        CollectionKnife knife = collectionKnifeRepository.findById(knifeId)
                .orElseThrow(() -> new Exception("Knife not found."));
        Collection userCollection = collectionRepository.findByUserId(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Collection not found for this account."));
        if (!knife.getCollectionId().equals(userCollection.getId())) {
            throw new Exception("Knife does not belong to your collection.");
        }
    }

    private <E extends Enum<E>> E parseKnifeEnum(Class<E> cls, String value) throws Exception {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(cls, value.toUpperCase().trim()); }
        catch (IllegalArgumentException e) { throw new Exception("Invalid " + cls.getSimpleName() + ": " + value); }
    }

    private List<TechniqueTag> parseTechniqueTags(List<String> techniqueTags, int maxCount) throws Exception {
        List<TechniqueTag> result = new ArrayList<>();
        if (techniqueTags == null) return result;
        if (techniqueTags.size() > maxCount) {
            throw new Exception("Maximum of " + maxCount + " technique tags allowed.");
        }
        for (String tag : techniqueTags) {
            try {
                result.add(TechniqueTag.valueOf(tag.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid technique tag: " + tag);
            }
        }
        return result;
    }
}
