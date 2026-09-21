package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.dtos.ConfirmEmailChangeDto;
import com.example.BalisongFlipping.dtos.ConfirmPasswordChangeDto;
import com.example.BalisongFlipping.dtos.DisplayNameChangeDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdatePreferencesDto;
import com.example.BalisongFlipping.dtos.UpdateSocialLinksDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.UserSearchResultDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.follows.Follow;
import com.example.BalisongFlipping.modals.follows.FollowId;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionKnifeRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.repositories.CommentLikeRepository;
import com.example.BalisongFlipping.repositories.CommentRepository;
import com.example.BalisongFlipping.repositories.EmailTokenRepository;
import com.example.BalisongFlipping.repositories.FollowRepository;
import com.example.BalisongFlipping.repositories.PostLikeRepository;
import com.example.BalisongFlipping.repositories.PostsRepository;
import com.example.BalisongFlipping.repositories.RefreshTokenRepository;
import com.example.BalisongFlipping.services.EmailService;
import com.example.BalisongFlipping.services.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplementationTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private CollectionKnifeRepository collectionKnifeRepository;
    @Mock private PostsRepository postsRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private EmailService emailService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private FollowRepository followRepository;
    @Mock private NotificationService notificationService;

    private AccountServiceImplementation accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImplementation();
        ReflectionTestUtils.setField(accountService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(accountService, "collectionRepository", collectionRepository);
        ReflectionTestUtils.setField(accountService, "collectionKnifeRepository", collectionKnifeRepository);
        ReflectionTestUtils.setField(accountService, "postsRepository", postsRepository);
        ReflectionTestUtils.setField(accountService, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(accountService, "emailTokenRepository", emailTokenRepository);
        ReflectionTestUtils.setField(accountService, "commentRepository", commentRepository);
        ReflectionTestUtils.setField(accountService, "postLikeRepository", postLikeRepository);
        ReflectionTestUtils.setField(accountService, "commentLikeRepository", commentLikeRepository);
        ReflectionTestUtils.setField(accountService, "emailService", emailService);
        ReflectionTestUtils.setField(accountService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(accountService, "followRepository", followRepository);
        ReflectionTestUtils.setField(accountService, "notificationService", notificationService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("flipper" + id + "@example.com");
        user.setDisplayName("Flipper" + id);
        user.setIdentifierCode("000" + id);
        user.setRole("USER");
        return user;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // -------------------------------------------------------------------------
    // toUserDto
    // -------------------------------------------------------------------------

    @Test
    void toUserDtoThrowsForNullAccount() {
        Exception ex = assertThrows(Exception.class, () -> accountService.toUserDto(null));
        assertEquals("Passed account is null.", ex.getMessage());
    }

    @Test
    void toUserDtoMapsFieldsAndFollowingIds() throws Exception {
        User u = user(1L);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(
                List.of(new Follow(new FollowId(1L, 2L)), new Follow(new FollowId(1L, 3L))));

        UserDto dto = accountService.toUserDto(u);

        assertEquals("1", dto.id());
        assertEquals("Flipper1", dto.displayName());
        assertEquals(java.util.Set.of(2L, 3L), dto.followingIds());
    }

    // -------------------------------------------------------------------------
    // generateIdentifierCode
    // -------------------------------------------------------------------------

    @Test
    void generateIdentifierCodeReturnsFourDigitCodeWhenNoConflicts() {
        when(accountRepository.findAllByDisplayName("Flipper")).thenReturn(List.of());

        String code = accountService.generateIdentifierCode("Flipper");

        assertEquals(4, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }

    // -------------------------------------------------------------------------
    // Account lookups
    // -------------------------------------------------------------------------

    @Test
    void getAccountReturnsAccountById() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        assertEquals("Flipper1", ((User) accountService.getAccount("1")).getDisplayName());
    }

    @Test
    void getAccountThrowsForUnknownId() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        Exception ex = assertThrows(Exception.class, () -> accountService.getAccount("999"));
        assertEquals("Account not found.", ex.getMessage());
    }

    @Test
    void getSelfRefetchesAndConvertsFreshAccount() throws Exception {
        User principal = user(1L);
        authenticateAs(principal);
        User fresh = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fresh));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        UserDto dto = accountService.getSelf();

        assertEquals("1", dto.id());
    }

    @Test
    void getSelfThrowsWhenAccountNoLongerExists() {
        authenticateAs(user(1L));
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> accountService.getSelf());
    }

    @Test
    void allUsersReturnsRepositoryResult() {
        when(accountRepository.findAll()).thenReturn(List.of(user(1L), user(2L)));
        assertEquals(2, accountService.allUsers().size());
    }

    @Test
    void checkForAccountExistanceReturnsTrueWhenPresent() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        assertTrue(accountService.checkForAccountExistance("1"));
    }

    @Test
    void checkForAccountExistanceReturnsFalseWhenAbsent() throws Exception {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        assertFalse(accountService.checkForAccountExistance("999"));
    }

    @Test
    void verifyAccountEmailMarksEmailVerified() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        accountService.verifyAccountEmail(u);

        assertTrue(u.getEmailVerified());
        verify(accountRepository).save(u);
    }

    @Test
    void searchUsersReturnsEmptyListForBlankQuery() {
        assertTrue(accountService.searchUsers("  ").isEmpty());
        verify(accountRepository, never()).searchByDisplayNameOrIdentifierCode(any());
    }

    @Test
    void searchUsersMapsResults() {
        when(accountRepository.searchByDisplayNameOrIdentifierCode("flip")).thenReturn(List.of(user(1L)));

        List<UserSearchResultDto> results = accountService.searchUsers("flip");

        assertEquals(1, results.size());
        assertEquals("Flipper1", results.get(0).displayName());
    }

    @Test
    void getPublicProfileByIdReturnsProfile() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        PublicProfileDto profile = accountService.getPublicProfileById("1");
        assertEquals("Flipper1", profile.displayName());
    }

    @Test
    void getPublicProfileByHandleReturnsProfile() throws Exception {
        when(accountRepository.findByDisplayNameAndIdentifierCode("Flipper1", "0001")).thenReturn(Optional.of(user(1L)));
        PublicProfileDto profile = accountService.getPublicProfileByHandle("Flipper1", "0001");
        assertEquals("Flipper1", profile.displayName());
    }

    // -------------------------------------------------------------------------
    // Profile updates
    // -------------------------------------------------------------------------

    @Test
    void changeDisplayNameSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.findAllByDisplayName("NewName")).thenReturn(List.of());

        DisplayNameChangeDto result = accountService.changeDisplayName("1", "NewName");

        assertEquals("NewName", result.displayName());
        assertEquals("NewName", u.getDisplayName());
    }

    @Test
    void changeDisplayNameRejectsInvalidName() {
        Exception ex = assertThrows(Exception.class, () -> accountService.changeDisplayName("1", "ab"));
        assertEquals("Display name not valid.", ex.getMessage());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void updateBioSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.updateBio("1", "New bio");

        assertEquals("New bio", u.getBio());
    }

    @Test
    void updateBioRejectsTooLongBio() {
        String longBio = "a".repeat(151);
        Exception ex = assertThrows(Exception.class, () -> accountService.updateBio("1", longBio));
        assertEquals("Profile caption cannot exceed 150 characters.", ex.getMessage());
    }

    @Test
    void updateProfileImgSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        String result = accountService.updateProfileImg("1", "https://cdn/profile.png");

        assertEquals("https://cdn/profile.png", result);
        assertEquals("https://cdn/profile.png", u.getProfileImg());
    }

    @Test
    void updateBannerImgSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        String result = accountService.updateBannerImg("1", "https://cdn/banner.png");

        assertEquals("https://cdn/banner.png", result);
        assertEquals("https://cdn/banner.png", u.getBannerImg());
    }

    // -------------------------------------------------------------------------
    // Social links
    // -------------------------------------------------------------------------

    @Test
    void updateSocialLinksSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        UpdateSocialLinksDto dto = new UpdateSocialLinksDto(
                "https://facebook.com/flipper", null, null, null, null, null, "flipper@example.com", null);

        accountService.updateSocialLinks("1", dto);

        assertEquals("https://facebook.com/flipper", u.getFacebookLink());
        assertEquals("flipper@example.com", u.getPersonalEmailLink());
        assertEquals("", u.getTwitterLink());
    }

    @Test
    void updateSocialLinksRejectsNonHttpLink() {
        UpdateSocialLinksDto dto = new UpdateSocialLinksDto(
                "not-a-url", null, null, null, null, null, null, null);

        Exception ex = assertThrows(Exception.class, () -> accountService.updateSocialLinks("1", dto));
        assertEquals("Facebook link must start with http:// or https://", ex.getMessage());
    }

    @Test
    void updateSocialLinksRejectsInvalidPersonalEmail() {
        UpdateSocialLinksDto dto = new UpdateSocialLinksDto(
                null, null, null, null, null, null, "not-an-email", null);

        Exception ex = assertThrows(Exception.class, () -> accountService.updateSocialLinks("1", dto));
        assertEquals("Personal email must be a valid email address.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Preferences
    // -------------------------------------------------------------------------

    @Test
    void updatePreferencesOnlyUpdatesProvidedFields() throws Exception {
        User u = user(1L);
        u.setCurrency("USD");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.updatePreferences("1", new UpdatePreferencesDto("METRIC", null));

        assertEquals("METRIC", u.getMeasurementUnit());
        assertEquals("USD", u.getCurrency());
    }

    // -------------------------------------------------------------------------
    // Google auth — setInitialDisplayName
    // -------------------------------------------------------------------------

    @Test
    void setInitialDisplayNameSucceeds() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(accountRepository.findAllByDisplayName("Flipper")).thenReturn(List.of());
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.setInitialDisplayName("1", "Flipper");

        assertEquals("Flipper", u.getDisplayName());
    }

    @Test
    void setInitialDisplayNameRejectsBlankName() {
        Exception ex = assertThrows(Exception.class, () -> accountService.setInitialDisplayName("1", " "));
        assertEquals("Display name cannot be empty.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Follow / unfollow
    // -------------------------------------------------------------------------

    @Test
    void followAccountSucceeds() throws Exception {
        User follower = user(1L);
        User target = user(2L);
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.followAccount("1", "2");

        assertEquals(1, follower.getFollowingCount());
        assertEquals(1, target.getFollowerCount());
        verify(notificationService).send(eq(2L), eq(1L), any(), any(), eq(1L));
    }

    @Test
    void followAccountRejectsSelfFollow() {
        Exception ex = assertThrows(Exception.class, () -> accountService.followAccount("1", "1"));
        assertEquals("You cannot follow yourself.", ex.getMessage());
    }

    @Test
    void followAccountRejectsDuplicateFollow() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> accountService.followAccount("1", "2"));
        assertEquals("Already following this account.", ex.getMessage());
    }

    @Test
    void unfollowAccountSucceeds() throws Exception {
        User follower = user(1L);
        follower.setFollowingCount(1);
        User target = user(2L);
        target.setFollowerCount(1);
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.unfollowAccount("1", "2");

        assertEquals(0, follower.getFollowingCount());
        assertEquals(0, target.getFollowerCount());
    }

    @Test
    void unfollowAccountRejectsWhenNotFollowing() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> accountService.unfollowAccount("1", "2"));
        assertEquals("Not following this account.", ex.getMessage());
    }

    @Test
    void getFollowingFiltersDeletedAccounts() throws Exception {
        when(followRepository.findByIdFollowerId(1L)).thenReturn(
                List.of(new Follow(new FollowId(1L, 2L)), new Follow(new FollowId(1L, 3L))));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(accountRepository.findById(3L)).thenReturn(Optional.empty());

        List<UserSearchResultDto> result = accountService.getFollowing("1");

        assertEquals(1, result.size());
        assertEquals("Flipper2", result.get(0).displayName());
    }

    @Test
    void getFollowersFiltersDeletedAccounts() throws Exception {
        when(followRepository.findByIdFollowingId(1L)).thenReturn(List.of(new Follow(new FollowId(2L, 1L))));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        List<UserSearchResultDto> result = accountService.getFollowers("1");

        assertEquals(1, result.size());
    }

    @Test
    void isFollowingReturnsTrueWhenFollowExists() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);
        assertTrue(accountService.isFollowing("1", "2"));
    }

    @Test
    void isFollowingReturnsFalseOnMalformedId() {
        assertFalse(accountService.isFollowing("not-a-number", "2"));
    }

    // -------------------------------------------------------------------------
    // Post count maintenance
    // -------------------------------------------------------------------------

    @Test
    void incrementPostCountIncreasesByOne() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        accountService.incrementPostCount("1");

        assertEquals(1, u.getPostCount());
    }

    @Test
    void decrementPostCountFloorsAtZero() throws Exception {
        User u = user(1L);
        u.setPostCount(0);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        accountService.decrementPostCount("1");

        assertEquals(0, u.getPostCount());
    }

    // -------------------------------------------------------------------------
    // Email change
    // -------------------------------------------------------------------------

    @Test
    void requestEmailChangeSendsCode() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        accountService.requestEmailChange("1");

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        verify(emailTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq(u.getEmail()), anyString(), anyString());
    }

    @Test
    void confirmEmailChangeSucceeds() throws Exception {
        User u = user(1L);
        EmailVerificationToken token = new EmailVerificationToken(u);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(accountRepository.findAccountByEmail("new@example.com")).thenReturn(Optional.empty());
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.confirmEmailChange("1", new ConfirmEmailChangeDto("123456", "new@example.com"));

        assertEquals("new@example.com", u.getEmail());
        verify(emailTokenRepository).delete(token);
    }

    @Test
    void confirmEmailChangeRejectsInvalidEmailFormat() {
        Exception ex = assertThrows(Exception.class,
                () -> accountService.confirmEmailChange("1", new ConfirmEmailChangeDto("123456", "not-an-email")));
        assertEquals("New email is not valid.", ex.getMessage());
    }

    @Test
    void confirmEmailChangeRejectsEmailAlreadyInUse() {
        when(accountRepository.findAccountByEmail("taken@example.com")).thenReturn(Optional.of(user(2L)));

        Exception ex = assertThrows(Exception.class,
                () -> accountService.confirmEmailChange("1", new ConfirmEmailChangeDto("123456", "taken@example.com")));
        assertEquals("Email is already in use.", ex.getMessage());
    }

    @Test
    void confirmEmailChangeRejectsExpiredCode() {
        User u = user(1L);
        EmailVerificationToken token = new EmailVerificationToken(u);
        token.setExpiration(Instant.now().minusSeconds(60));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.findAccountByEmail("new@example.com")).thenReturn(Optional.empty());
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class,
                () -> accountService.confirmEmailChange("1", new ConfirmEmailChangeDto("123456", "new@example.com")));
        assertEquals("Code has expired. Please request a new one.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Password change
    // -------------------------------------------------------------------------

    @Test
    void requestPasswordChangeSendsCode() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));

        accountService.requestPasswordChange("1");

        verify(emailTokenRepository).deleteByOwner_Id(1L);
        verify(emailService).sendEmail(eq(u.getEmail()), anyString(), anyString());
    }

    @Test
    void confirmPasswordChangeSucceeds() throws Exception {
        User u = user(1L);
        EmailVerificationToken token = new EmailVerificationToken(u);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("hashed");

        accountService.confirmPasswordChange("1", new ConfirmPasswordChangeDto("123456", "new-password"));

        assertEquals("hashed", u.getPassword());
        verify(emailTokenRepository).delete(token);
    }

    @Test
    void confirmPasswordChangeRejectsShortPassword() {
        Exception ex = assertThrows(Exception.class,
                () -> accountService.confirmPasswordChange("1", new ConfirmPasswordChangeDto("123456", "short")));
        assertEquals("Password must be at least 7 characters.", ex.getMessage());
    }

    @Test
    void confirmPasswordChangeRejectsCodeBelongingToDifferentAccount() {
        User u = user(1L);
        User other = user(2L);
        EmailVerificationToken token = new EmailVerificationToken(other);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(emailTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(Exception.class,
                () -> accountService.confirmPasswordChange("1", new ConfirmPasswordChangeDto("123456", "new-password")));
        assertEquals("Invalid code.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Danger zone
    // -------------------------------------------------------------------------

    @Test
    void hideAccountTogglesHiddenFlag() throws Exception {
        User u = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.hideAccount("1");
        assertTrue(u.isHidden());
    }

    @Test
    void resetAccountWipesProfileAndDeletesKnivesAndPosts() throws Exception {
        User u = user(1L);
        u.setBio("Old bio");
        u.setCollectionId(5L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.resetAccount("1");

        assertEquals("", u.getBio());
        assertEquals("IMPERIAL", u.getMeasurementUnit());
        verify(collectionKnifeRepository).deleteAllByCollectionId(5L);
        verify(postsRepository).deleteAllByAccountId("1");
    }

    @Test
    void resetAccountSkipsKnifeDeletionWhenNoCollection() throws Exception {
        User u = user(1L);
        u.setCollectionId(null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepository.save(u)).thenReturn(u);
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        accountService.resetAccount("1");

        verify(collectionKnifeRepository, never()).deleteAllByCollectionId(any());
    }

    @Test
    void deleteAccountRemovesAllRelatedData() throws Exception {
        accountService.deleteAccount("1");

        verify(refreshTokenRepository).deleteByOwner_Id(1L);
        verify(emailTokenRepository).deleteByOwner_Id(1L);
        verify(postsRepository).anonymizeByAccountId("1");
        verify(commentRepository).anonymizeByAccountId(1L);
        verify(postLikeRepository).deleteAllById_AccountId(1L);
        verify(commentLikeRepository).deleteAllById_AccountId(1L);
        verify(accountRepository).deleteById(1L);
    }
}
