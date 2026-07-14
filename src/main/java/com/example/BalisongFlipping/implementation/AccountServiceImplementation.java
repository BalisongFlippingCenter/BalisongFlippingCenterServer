package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.dtos.*;
import com.example.BalisongFlipping.dtos.ConfirmEmailChangeDto;
import com.example.BalisongFlipping.dtos.ConfirmPasswordChangeDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.utils.ProfanityFilter;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.follows.Follow;
import com.example.BalisongFlipping.modals.follows.FollowId;
import com.example.BalisongFlipping.services.NotificationService;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.*;
import com.example.BalisongFlipping.services.EmailService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.BalisongFlipping.repositories.CommentRepository;
import com.example.BalisongFlipping.repositories.PostLikeRepository;
import com.example.BalisongFlipping.repositories.CommentLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountServiceImplementation implements com.example.BalisongFlipping.services.AccountService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private CollectionKnifeRepository collectionKnifeRepository;
    @Autowired private PostsRepository postsRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private FollowRepository followRepository;
    @Autowired private NotificationService notificationService;

    // -------------------------------------------------------------------------
    // DTO conversion
    // -------------------------------------------------------------------------

    @Override
    public UserDto toUserDto(Account account) throws Exception {
        if (account == null) throw new Exception("Passed account is null.");

        User user = (User) account;
        Set<Long> followingIds = followRepository.findByIdFollowerId(account.getId())
                .stream().map(f -> f.getId().getFollowingId()).collect(java.util.stream.Collectors.toSet());

        return new UserDto(
                account.getId().toString(),
                account.getEmail(),
                account.getEmailVerified(),
                user.getDisplayName(),
                user.getIdentifierCode(),
                "USER",
                user.getCollectionId() != null ? user.getCollectionId().toString() : null,
                user.getBannerImg(),
                user.getProfileImg(),
                user.getBio(),
                user.getMeasurementUnit(),
                user.getCurrency(),
                user.isHidden(),
                user.getFacebookLink(),
                user.getTwitterLink(),
                user.getInstagramLink(),
                user.getYoutubeLink(),
                user.getDiscordLink(),
                user.getRedditLink(),
                user.getPersonalEmailLink(),
                user.getPersonalWebsiteLink(),
                user.getLikedPostIds(),
                user.getLikedCommentIds(),
                followingIds,
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount());
    }

    // -------------------------------------------------------------------------
    // Identifier code
    // -------------------------------------------------------------------------

    @Override
    public String generateIdentifierCode(String displayName) {
        StringBuilder identifier = new StringBuilder();

        do {
            if (!identifier.isEmpty()) identifier.delete(0, identifier.length());
            Random rand = new Random();
            for (int i = 0; i < 4; i++) identifier.append(rand.nextInt(10));
        } while (!validateGeneratedIdentifierCode(identifier.toString(), displayName));

        return identifier.toString();
    }

    private boolean validateGeneratedIdentifierCode(String identifierCode, String displayName) {
        List<User> foundAccounts = accountRepository.findAllByDisplayName(displayName);
        if (foundAccounts.isEmpty()) return true;
        for (User account : foundAccounts) {
            if (account.getIdentifierCode().equals(identifierCode)) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Account lookups
    // -------------------------------------------------------------------------

    @Override
    public Account getAccount(String accountId) throws Exception {
        return accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
    }

    @Override
    public UserDto getSelf() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Account currentAccount = (Account) authentication.getPrincipal();
        return toUserDto(currentAccount);
    }

    @Override
    public Account getAccountByEmail(String userEmail) throws Exception {
        return accountRepository.findAccountByEmail(userEmail)
                .orElseThrow(() -> new Exception("Account not found."));
    }

    @Override
    public List<Account> allUsers() {
        return accountRepository.findAll();
    }

    @Override
    public Boolean checkForAccountExistance(String accountId) throws Exception {
        return accountRepository.findById(Long.parseLong(accountId)).isPresent();
    }

    @Override
    public void verifyAccountEmail(Account user) throws Exception {
        Account found = accountRepository.findById(user.getId())
                .orElseThrow(() -> new Exception("User Not Found."));
        found.setEmailVerified(true);
        accountRepository.save(found);
    }

    @Override
    public List<UserSearchResultDto> searchUsers(String query) {
        if (query == null || query.isBlank()) return List.of();
        return accountRepository.searchByDisplayNameOrIdentifierCode(query.trim()).stream()
                .map(u -> new UserSearchResultDto(
                        String.valueOf(u.getId()),
                        u.getDisplayName(),
                        u.getIdentifierCode(),
                        u.getProfileImg(),
                        u.getBio()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PublicProfileDto getPublicProfileById(String accountId) throws Exception {
        Account account = accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        return convertToPublicProfileDto((User) account);
    }

    @Override
    public PublicProfileDto getPublicProfileByHandle(String displayName, String identifierCode) throws Exception {
        User user = accountRepository.findByDisplayNameAndIdentifierCode(displayName, identifierCode)
                .orElseThrow(() -> new Exception("Account not found."));
        return convertToPublicProfileDto(user);
    }

    private static PublicProfileDto convertToPublicProfileDto(User user) {
        return new PublicProfileDto(
                user.getId().toString(),
                user.getDisplayName(),
                user.getIdentifierCode(),
                user.getProfileImg(),
                user.getBannerImg(),
                user.getBio(),
                user.getCollectionId() != null ? user.getCollectionId().toString() : null,
                user.isHidden(),
                user.getFacebookLink(),
                user.getTwitterLink(),
                user.getInstagramLink(),
                user.getYoutubeLink(),
                user.getDiscordLink(),
                user.getRedditLink(),
                user.getPersonalEmailLink(),
                user.getPersonalWebsiteLink(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount()
        );
    }

    // -------------------------------------------------------------------------
    // Profile updates
    // -------------------------------------------------------------------------

    @Override
    public DisplayNameChangeDto changeDisplayName(String accountId, String newDisplayName) throws Exception {
        if (!validateDisplayName(newDisplayName))
            throw new Exception("Display name not valid.");

        User user = getUser(accountId);
        user.setDisplayName(newDisplayName);

        String newIdentifier = generateIdentifierCode(newDisplayName);
        user.setIdentifierCode(newIdentifier);

        accountRepository.save(user);
        return new DisplayNameChangeDto(newDisplayName, newIdentifier);
    }

    @Override
    public UserDto updateBio(String accountId, String bio) throws Exception {
        if (bio != null && bio.length() > 150)
            throw new Exception("Profile caption cannot exceed 150 characters.");
        User user = getUser(accountId);
        user.setBio(bio != null ? bio : "");
        return toUserDto(accountRepository.save(user));
    }

    @Override
    public String updateProfileImg(String accountId, String imageKey) throws Exception {
        User user = getUser(accountId);
        user.setProfileImg(imageKey);
        accountRepository.save(user);
        return imageKey;
    }

    @Override
    public String updateBannerImg(String accountId, String imageKey) throws Exception {
        User user = getUser(accountId);
        user.setBannerImg(imageKey);
        accountRepository.save(user);
        return imageKey;
    }

    // -------------------------------------------------------------------------
    // Social links (consolidated)
    // -------------------------------------------------------------------------

    @Override
    public UserDto updateSocialLinks(String accountId, UpdateSocialLinksDto dto) throws Exception {
        validateLinkField("Facebook",        dto.facebookLink(),       false);
        validateLinkField("Twitter",         dto.twitterLink(),        false);
        validateLinkField("Instagram",       dto.instagramLink(),      false);
        validateLinkField("YouTube",         dto.youtubeLink(),        false);
        validateLinkField("Discord",         dto.discordLink(),        false);
        validateLinkField("Reddit",          dto.redditLink(),         false);
        validateLinkField("Personal email",  dto.personalEmailLink(),  true);
        validateLinkField("Personal website",dto.personalWebsiteLink(),false);

        User user = getUser(accountId);
        user.setFacebookLink(dto.facebookLink() != null ? dto.facebookLink() : "");
        user.setTwitterLink(dto.twitterLink() != null ? dto.twitterLink() : "");
        user.setInstagramLink(dto.instagramLink() != null ? dto.instagramLink() : "");
        user.setYoutubeLink(dto.youtubeLink() != null ? dto.youtubeLink() : "");
        user.setDiscordLink(dto.discordLink() != null ? dto.discordLink() : "");
        user.setRedditLink(dto.redditLink() != null ? dto.redditLink() : "");
        user.setPersonalEmailLink(dto.personalEmailLink() != null ? dto.personalEmailLink() : "");
        user.setPersonalWebsiteLink(dto.personalWebsiteLink() != null ? dto.personalWebsiteLink() : "");
        return toUserDto(accountRepository.save(user));
    }

    private void validateLinkField(String fieldName, String value, boolean isEmail) throws Exception {
        if (value == null || value.isEmpty()) return; // empty = clear the link, always valid
        if (isEmail) {
            if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                throw new Exception(fieldName + " must be a valid email address.");
        } else {
            if (!value.startsWith("http://") && !value.startsWith("https://"))
                throw new Exception(fieldName + " link must start with http:// or https://");
        }
    }

    // -------------------------------------------------------------------------
    // App preferences
    // -------------------------------------------------------------------------

    @Override
    public UserDto updatePreferences(String accountId, UpdatePreferencesDto dto) throws Exception {
        User user = getUser(accountId);
        if (dto.measurementUnit() != null) user.setMeasurementUnit(dto.measurementUnit());
        if (dto.currency() != null) user.setCurrency(dto.currency());
        return toUserDto(accountRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // Google auth
    // -------------------------------------------------------------------------

    @Override
    public UserDto setInitialDisplayName(String accountId, String displayName) throws Exception {
        if (displayName == null || displayName.isBlank())
            throw new Exception("Display name cannot be empty.");
        if (!validateDisplayName(displayName))
            throw new Exception("Display name must be at least 4 characters and contain only letters, numbers, _, !, or .");

        User user = getUser(accountId);
        user.setDisplayName(displayName);
        user.setIdentifierCode(generateIdentifierCode(displayName));
        return toUserDto(accountRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // Follow / unfollow
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UserDto followAccount(String followerId, String targetId) throws Exception {
        if (followerId.equals(targetId))
            throw new Exception("You cannot follow yourself.");

        Long followerLong = Long.parseLong(followerId);
        Long targetLong   = Long.parseLong(targetId);

        FollowId followId = new FollowId(followerLong, targetLong);
        if (followRepository.existsById(followId))
            throw new Exception("Already following this account.");

        followRepository.save(new Follow(followId));

        User follower = getUser(followerId);
        follower.setFollowingCount(follower.getFollowingCount() + 1);
        accountRepository.save(follower);

        User target = getUser(targetId);
        target.setFollowerCount(target.getFollowerCount() + 1);
        accountRepository.save(target);

        notificationService.send(targetLong, followerLong, NotificationType.NEW_FOLLOWER, TargetType.PROFILE, followerLong);

        return toUserDto(follower);
    }

    @Override
    @Transactional
    public UserDto unfollowAccount(String followerId, String targetId) throws Exception {
        Long followerLong = Long.parseLong(followerId);
        Long targetLong   = Long.parseLong(targetId);

        FollowId followId = new FollowId(followerLong, targetLong);
        if (!followRepository.existsById(followId))
            throw new Exception("Not following this account.");

        followRepository.deleteById(followId);

        User follower = getUser(followerId);
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
        accountRepository.save(follower);

        User target = getUser(targetId);
        target.setFollowerCount(Math.max(0, target.getFollowerCount() - 1));
        accountRepository.save(target);

        return toUserDto(follower);
    }

    @Override
    public List<UserSearchResultDto> getFollowing(String accountId) throws Exception {
        Long uid = Long.parseLong(accountId);
        return followRepository.findByIdFollowerId(uid).stream()
                .map(f -> {
                    User u = accountRepository.findById(f.getId().getFollowingId())
                            .map(a -> (User) a).orElse(null);
                    if (u == null) return null;
                    return new UserSearchResultDto(u.getId().toString(), u.getDisplayName(),
                            u.getIdentifierCode(), u.getProfileImg(), u.getBio());
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Override
    public List<UserSearchResultDto> getFollowers(String accountId) throws Exception {
        Long uid = Long.parseLong(accountId);
        return followRepository.findByIdFollowingId(uid).stream()
                .map(f -> {
                    User u = accountRepository.findById(f.getId().getFollowerId())
                            .map(a -> (User) a).orElse(null);
                    if (u == null) return null;
                    return new UserSearchResultDto(u.getId().toString(), u.getDisplayName(),
                            u.getIdentifierCode(), u.getProfileImg(), u.getBio());
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Override
    public boolean isFollowing(String followerId, String targetId) {
        try {
            return followRepository.existsById(
                    new FollowId(Long.parseLong(followerId), Long.parseLong(targetId)));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void incrementPostCount(String accountId) throws Exception {
        User user = getUser(accountId);
        user.setPostCount(user.getPostCount() + 1);
        accountRepository.save(user);
    }

    @Override
    @Transactional
    public void decrementPostCount(String accountId) throws Exception {
        User user = getUser(accountId);
        user.setPostCount(Math.max(0, user.getPostCount() - 1));
        accountRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Email / password change (2-step via email code)
    // -------------------------------------------------------------------------

    @Override
    public void requestEmailChange(String accountId) throws Exception {
        User user = getUser(accountId);
        emailTokenRepository.deleteByOwner_Id(user.getId());

        EmailVerificationToken token = new EmailVerificationToken(user);
        emailTokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                "Balisong Flipping Hub — Email Change Code",
                "Your verification code is: " + token.getToken() + "\n\nThis code expires in 10 minutes."
        );
    }

    @Override
    @Transactional
    public UserDto confirmEmailChange(String accountId, ConfirmEmailChangeDto dto) throws Exception {
        if (dto.newEmail() == null || !dto.newEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new Exception("New email is not valid.");

        if (accountRepository.findAccountByEmail(dto.newEmail()).isPresent())
            throw new Exception("Email is already in use.");

        User user = getUser(accountId);

        EmailVerificationToken token = emailTokenRepository.findByToken(dto.code())
                .orElseThrow(() -> new Exception("Invalid code."));

        if (!token.getOwner().getId().equals(user.getId()))
            throw new Exception("Invalid code.");

        if (token.getExpiration().isBefore(java.time.Instant.now()))
            throw new Exception("Code has expired. Please request a new one.");

        emailTokenRepository.delete(token);
        user.setEmail(dto.newEmail());
        return toUserDto(accountRepository.save(user));
    }

    @Override
    public void requestPasswordChange(String accountId) throws Exception {
        User user = getUser(accountId);
        emailTokenRepository.deleteByOwner_Id(user.getId());

        EmailVerificationToken token = new EmailVerificationToken(user);
        emailTokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                "Balisong Flipping Hub — Password Change Code",
                "Your verification code is: " + token.getToken() + "\n\nThis code expires in 10 minutes."
        );
    }

    @Override
    @Transactional
    public void confirmPasswordChange(String accountId, ConfirmPasswordChangeDto dto) throws Exception {
        if (dto.newPassword() == null || dto.newPassword().length() < 7)
            throw new Exception("Password must be at least 7 characters.");

        User user = getUser(accountId);

        EmailVerificationToken token = emailTokenRepository.findByToken(dto.code())
                .orElseThrow(() -> new Exception("Invalid code."));

        if (!token.getOwner().getId().equals(user.getId()))
            throw new Exception("Invalid code.");

        if (token.getExpiration().isBefore(java.time.Instant.now()))
            throw new Exception("Code has expired. Please request a new one.");

        emailTokenRepository.delete(token);
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        accountRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Danger zone
    // -------------------------------------------------------------------------

    @Override
    public UserDto hideAccount(String accountId) throws Exception {
        User user = getUser(accountId);
        user.setHidden(!user.isHidden());
        return toUserDto(accountRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto resetAccount(String accountId) throws Exception {
        User user = getUser(accountId);

        // wipe profile data
        user.setBio("");
        user.setProfileImg("");
        user.setBannerImg("");
        user.setMeasurementUnit("IMPERIAL");
        user.setCurrency("USD");
        user.setHidden(false);

        // wipe social links
        user.setFacebookLink("");
        user.setTwitterLink("");
        user.setInstagramLink("");
        user.setYoutubeLink("");
        user.setDiscordLink("");
        user.setRedditLink("");
        user.setPersonalEmailLink("");
        user.setPersonalWebsiteLink("");

        // wipe collection knives
        if (user.getCollectionId() != null) {
            collectionKnifeRepository.deleteAllByCollectionId(user.getCollectionId());
        }

        // wipe posts
        postsRepository.deleteAllByAccountId(accountId);

        return toUserDto(accountRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(String accountId) throws Exception {
        Long userId = Long.parseLong(accountId);

        // remove auth tokens
        refreshTokenRepository.deleteByOwner_Id(userId);
        emailTokenRepository.deleteByOwner_Id(userId);

        // remove collection knives then collection (personal property — not kept)
        collectionRepository.findByUserId(userId).ifPresent(collection -> {
            collectionKnifeRepository.deleteAllByCollectionId(collection.getId());
            collectionRepository.delete(collection);
        });

        // anonymize posts and comments — content stays, author becomes null
        postsRepository.anonymizeByAccountId(accountId);
        commentRepository.anonymizeByAccountId(userId);

        // remove like records for this account (counts on posts/comments are intentionally left as-is)
        postLikeRepository.deleteAllById_AccountId(userId);
        commentLikeRepository.deleteAllById_AccountId(userId);

        // remove account (cascade-deletes account_liked_posts and account_liked_comments)
        accountRepository.deleteById(userId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User getUser(String accountId) throws Exception {
        return (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
    }

    private boolean validateDisplayName(String name) {
        if (name == null || name.length() < 4) return false;

        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '!' && c != '.') return false;
        }

        if (ProfanityFilter.containsProfanity(name)) return false;
        
        return true;
    }
}
