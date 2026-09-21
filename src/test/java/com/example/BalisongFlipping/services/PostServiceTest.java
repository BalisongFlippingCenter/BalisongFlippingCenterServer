package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.postsDtos.CreatePostRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostMediaInputDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostResponseDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.UpdatePostDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.modals.posts.BuySellPost;
import com.example.BalisongFlipping.modals.posts.GenericPost;
import com.example.BalisongFlipping.modals.posts.PostWrapper;
import com.example.BalisongFlipping.modals.posts.TradePost;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.repositories.PostLikeRepository;
import com.example.BalisongFlipping.repositories.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostsRepository postsRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CollectionKnifeRepository collectionKnifeRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private AccountService accountService;
    @Mock private NotificationService notificationService;
    @Mock private S3Service s3Service;

    private PostService postService;

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "us-east-1";

    @BeforeEach
    void setUp() {
        postService = new PostService();
        ReflectionTestUtils.setField(postService, "postsRepository", postsRepository);
        ReflectionTestUtils.setField(postService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(postService, "collectionKnifeRepository", collectionKnifeRepository);
        ReflectionTestUtils.setField(postService, "collectionRepository", collectionRepository);
        ReflectionTestUtils.setField(postService, "postLikeRepository", postLikeRepository);
        ReflectionTestUtils.setField(postService, "accountService", accountService);
        ReflectionTestUtils.setField(postService, "notificationService", notificationService);
        ReflectionTestUtils.setField(postService, "s3Service", s3Service);
        ReflectionTestUtils.setField(postService, "bucketName", BUCKET);
        ReflectionTestUtils.setField(postService, "s3Region", REGION);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setDisplayName("Flipper" + id);
        user.setIdentifierCode("000" + id);
        return user;
    }

    private String mediaUrl(String accountId, String filename) {
        return "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/posts/" + accountId + "/gen/uuid/" + filename;
    }

    private PostMediaInputDto imageMedia(String accountId) {
        return new PostMediaInputDto(mediaUrl(accountId, "cover.png"), null, null);
    }

    private PostMediaInputDto videoMedia(String accountId) {
        return new PostMediaInputDto(mediaUrl(accountId, "clip.mp4"), null, null);
    }

    private GenericPost genericPost(Long id, String accountId, String caption) {
        GenericPost post = new GenericPost();
        post.setId(id);
        post.setAccountId(accountId);
        post.setCaption(caption);
        return post;
    }

    // -------------------------------------------------------------------------
    // getPostById
    // -------------------------------------------------------------------------

    @Test
    void getPostByIdReturnsPostWithAuthor() throws Exception {
        GenericPost post = genericPost(5L, "1", "Nice flip");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        PostResponseDto dto = postService.getPostById(5L);

        assertEquals("Flipper1", dto.author().displayName());
    }

    @Test
    void getPostByIdThrowsForUnknownId() {
        when(postsRepository.findById(999L)).thenReturn(Optional.empty());
        Exception ex = assertThrows(Exception.class, () -> postService.getPostById(999L));
        assertEquals("Post not found.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // getPosts
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void getPostsDelegatesToRepositoryAndMapsResults() throws Exception {
        GenericPost post = genericPost(1L, "1", "Nice flip");
        when(postsRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostResponseDto> result = postService.getPosts(
                null, null, null, null, 0, 20, null,
                null, null, null, null, null, null, null, null, null, null);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPostsRejectsUnknownPostType() {
        Exception ex = assertThrows(Exception.class, () -> postService.getPosts(
                "BOGUS", null, null, null, 0, 20, null,
                null, null, null, null, null, null, null, null, null, null));
        assertEquals("Unknown postType: BOGUS", ex.getMessage());
    }

    @Test
    void getPostsRejectsInvalidKnifeFilterEnum() {
        Exception ex = assertThrows(Exception.class, () -> postService.getPosts(
                null, null, null, null, 0, 20, null,
                "NOT_A_STYLE", null, null, null, null, null, null, null, null, null));
        assertTrue(ex.getMessage().contains("Invalid BladeStyle"));
    }

    // -------------------------------------------------------------------------
    // getLikedPosts
    // -------------------------------------------------------------------------

    @Test
    void getLikedPostsReturnsEmptyPageWhenNoLikes() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        Page<PostResponseDto> result = postService.getLikedPosts("1", 0, 20);

        assertTrue(result.isEmpty());
        verify(postsRepository, never()).findByIdIn(any(), any());
    }

    @Test
    void getLikedPostsDelegatesWhenLikesPresent() throws Exception {
        User u = user(1L);
        u.getLikedPostIds().add(5L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(postsRepository.findByIdIn(eq(u.getLikedPostIds()), any())).thenReturn(new PageImpl<>(List.of(genericPost(5L, "1", "Liked"))));

        Page<PostResponseDto> result = postService.getLikedPosts("1", 0, 20);

        assertEquals(1, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // likePost / unlikePost
    // -------------------------------------------------------------------------

    @Test
    void likePostSucceeds() throws Exception {
        GenericPost post = genericPost(5L, "2", "Nice flip");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(false);
        User liker = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(liker));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        PostResponseDto result = postService.likePost(5L, "1");

        assertEquals(1, post.getLikeCount());
        assertTrue(liker.getLikedPostIds().contains(5L));
        verify(notificationService).send(eq(2L), eq(1L), any(), any(), eq(5L));
    }

    @Test
    void likePostRejectsDuplicateLike() {
        GenericPost post = genericPost(5L, "2", "Nice flip");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> postService.likePost(5L, "1"));
        assertEquals("Post already liked.", ex.getMessage());
    }

    @Test
    void unlikePostSucceeds() throws Exception {
        GenericPost post = genericPost(5L, "2", "Nice flip");
        post.setLikeCount(1);
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(true);
        User liker = user(1L);
        liker.getLikedPostIds().add(5L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(liker));

        postService.unlikePost(5L, "1");

        assertEquals(0, post.getLikeCount());
        assertTrue(liker.getLikedPostIds().isEmpty());
    }

    @Test
    void unlikePostRejectsWhenNotLiked() {
        GenericPost post = genericPost(5L, "2", "Nice flip");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> postService.unlikePost(5L, "1"));
        assertEquals("Post not liked.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // generateUploadUrls
    // -------------------------------------------------------------------------

    @Test
    void generateUploadUrlsSucceeds() throws Exception {
        when(s3Service.generatePresignedUploadUrl(eq(BUCKET), any(), any(), any())).thenReturn("https://upload");

        List<PresignedUploadTargetDto> targets = postService.generateUploadUrls("1",
                new PostUploadUrlRequestDto("GENERIC", List.of(new FileUploadRequestItem("cover.png", "image/png"))));

        assertEquals(1, targets.size());
        assertEquals("https://upload", targets.get(0).uploadUrl());
    }

    @Test
    void generateUploadUrlsRejectsMissingPostType() {
        Exception ex = assertThrows(Exception.class, () -> postService.generateUploadUrls("1",
                new PostUploadUrlRequestDto(null, List.of(new FileUploadRequestItem("a.png", "image/png")))));
        assertEquals("postType is required.", ex.getMessage());
    }

    @Test
    void generateUploadUrlsRejectsEmptyFiles() {
        Exception ex = assertThrows(Exception.class, () -> postService.generateUploadUrls("1",
                new PostUploadUrlRequestDto("GENERIC", List.of())));
        assertEquals("At least one file is required.", ex.getMessage());
    }

    @Test
    void generateUploadUrlsRejectsTooManyFiles() {
        List<FileUploadRequestItem> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> new FileUploadRequestItem(i + ".png", "image/png"))
                .toList();

        Exception ex = assertThrows(Exception.class, () -> postService.generateUploadUrls("1",
                new PostUploadUrlRequestDto("GENERIC", files)));
        assertEquals("Cannot request more than 10 upload URLs at once.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // createPost — gating + routing
    // -------------------------------------------------------------------------

    private User accountForCreate(Long id, boolean muted) {
        User u = user(id);
        if (muted) u.setMutedUntil(java.time.Instant.now().plusSeconds(3600));
        return u;
    }

    @Test
    void createPostRejectsMutedAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, true)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("GENERIC", "caption", null, null, List.of(imageMedia("1")), null, null, null, null, null, null, null)));
        assertTrue(ex.getMessage().startsWith("You are muted until"));
    }

    @Test
    void createPostRejectsUnknownPostType() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("BOGUS", "caption", null, null, List.of(imageMedia("1")), null, null, null, null, null, null, null)));
        assertEquals("Unknown postType: BOGUS", ex.getMessage());
    }

    @Test
    void createPostGenericSucceedsAndIncrementsPostCount() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));
        when(s3Service.doesObjectExist(eq(BUCKET), any())).thenReturn(true);
        when(postsRepository.save(any(PostWrapper.class))).thenAnswer(invocation -> {
            PostWrapper p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PostWrapper result = postService.createPost("1",
                new CreatePostRequestDto("GENERIC", "caption", null, null, List.of(imageMedia("1")), null, null, null, null, List.of("FLIPPING"), null, null));

        assertEquals("caption", result.getCaption());
        verify(accountService).incrementPostCount("1");
    }

    @Test
    void createGenericPostRejectsInvalidTag() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));
        when(s3Service.doesObjectExist(eq(BUCKET), any())).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("GENERIC", "caption", null, null, List.of(imageMedia("1")), null, null, null, null, List.of("NOT_A_TAG"), null, null)));
        assertEquals("Invalid generic tag: NOT_A_TAG", ex.getMessage());
    }

    @Test
    void createBuySellPostBuyingRejectsVideoMedia() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));
        when(s3Service.doesObjectExist(eq(BUCKET), any())).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("BUY_SELL", "caption", null, null, List.of(videoMedia("1")), "BUYING", null, null, null, null, null, null)));
        assertEquals("Buying posts require an image, not a video.", ex.getMessage());
    }

    @Test
    void createBuySellPostRejectsInvalidMode() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("BUY_SELL", "caption", null, null, List.of(imageMedia("1")), "BOGUS", null, null, null, null, null, null)));
        assertTrue(ex.getMessage().startsWith("Invalid mode:"));
    }

    @Test
    void createBuySellPostSellingRequiresOfferingKnife() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("BUY_SELL", "caption", null, null, List.of(imageMedia("1")), "SELLING", null, null, null, null, null, null)));
        assertEquals("offeringKnifeId is required when selling.", ex.getMessage());
    }

    @Test
    void createBuySellPostSellingRejectsUnownedKnife() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));
        CollectionKnife knife = new CollectionKnife();
        knife.setId(7L);
        knife.setCollectionId(99L);
        when(collectionKnifeRepository.findById(7L)).thenReturn(Optional.of(knife));
        Collection otherCollection = new Collection();
        otherCollection.setId(5L);
        when(collectionRepository.findByUserId(1L)).thenReturn(Optional.of(otherCollection));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("BUY_SELL", "caption", null, null, List.of(imageMedia("1")), "SELLING", "7", null, null, null, null, null)));
        assertEquals("Knife does not belong to your collection.", ex.getMessage());
    }

    @Test
    void createTradePostRequiresOfferingKnifeAndLookingForText() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("TRADE", "caption", null, null, List.of(imageMedia("1")), null, null, null, null, null, null, null)));
        assertEquals("offeringKnifeId is required for trade posts.", ex.getMessage());
    }

    @Test
    void createTrickTutorialPostRequiresVideo() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));
        when(s3Service.doesObjectExist(eq(BUCKET), any())).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("TRICK_TUTORIAL", "Cool trick", null, null, List.of(imageMedia("1")), null, null, null, null, null, "BEGINNER", null)));
        assertEquals("Trick tutorial posts require a video file.", ex.getMessage());
    }

    @Test
    void createComboPostRequiresDifficultyTag() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountForCreate(1L, false)));

        Exception ex = assertThrows(Exception.class, () -> postService.createPost("1",
                new CreatePostRequestDto("COMBO", null, null, null, List.of(videoMedia("1")), null, null, null, null, null, null, null)));
        assertEquals("difficultyTag is required for combo posts.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // updatePost
    // -------------------------------------------------------------------------

    @Test
    void updatePostSucceeds() throws Exception {
        GenericPost post = genericPost(5L, "1", "Old caption");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postsRepository.save(post)).thenReturn(post);

        PostResponseDto result = postService.updatePost(5L, "1", new UpdatePostDto("New caption", null, true, null, null));

        assertEquals("New caption", result.post().getCaption());
        assertTrue(result.post().isPrivate());
    }

    @Test
    void updatePostRejectsNonOwner() {
        GenericPost post = genericPost(5L, "1", "Old caption");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));

        Exception ex = assertThrows(Exception.class, () -> postService.updatePost(5L, "2", new UpdatePostDto("New caption", null, null, null, null)));
        assertEquals("You do not own this post.", ex.getMessage());
    }

    @Test
    void updatePostThrowsForUnknownId() {
        when(postsRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> postService.updatePost(999L, "1", new UpdatePostDto(null, null, null, null, null)));
    }

    // -------------------------------------------------------------------------
    // deletePost
    // -------------------------------------------------------------------------

    @Test
    void deletePostSucceeds() throws Exception {
        GenericPost post = genericPost(5L, "1", "Old caption");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));

        postService.deletePost(5L, "1");

        verify(postsRepository).delete(post);
        verify(accountService).decrementPostCount("1");
    }

    @Test
    void deletePostRejectsNonOwner() {
        GenericPost post = genericPost(5L, "1", "Old caption");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post));

        Exception ex = assertThrows(Exception.class, () -> postService.deletePost(5L, "2"));
        assertEquals("You do not own this post.", ex.getMessage());
        verify(postsRepository, never()).delete(any(PostWrapper.class));
    }
}
