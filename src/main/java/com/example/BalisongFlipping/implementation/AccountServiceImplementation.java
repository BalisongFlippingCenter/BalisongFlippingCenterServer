package com.example.BalisongFlipping.implementation;

import com.example.BalisongFlipping.dtos.*;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.utils.ProfanityFilter;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountServiceImplementation implements com.example.BalisongFlipping.services.AccountService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private CollectionKnifeRepository collectionKnifeRepository;
    @Autowired private PostsRepository postsRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;

    // -------------------------------------------------------------------------
    // DTO conversion
    // -------------------------------------------------------------------------

    public static UserDto convertAccountToDto(Account account) throws Exception {
        if (account == null) throw new Exception("Passed account is null.");

        User user = (User) account;
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
                user.getPersonalWebsiteLink());
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
        return convertAccountToDto(currentAccount);
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
                user.getPersonalWebsiteLink()
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
        return convertAccountToDto(accountRepository.save(user));
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
        return convertAccountToDto(accountRepository.save(user));
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
        return convertAccountToDto(accountRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // Danger zone
    // -------------------------------------------------------------------------

    @Override
    public UserDto hideAccount(String accountId) throws Exception {
        User user = getUser(accountId);
        user.setHidden(!user.isHidden());
        return convertAccountToDto(accountRepository.save(user));
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

        return convertAccountToDto(accountRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(String accountId) throws Exception {
        Long userId = Long.parseLong(accountId);

        // remove auth tokens
        refreshTokenRepository.deleteByOwner_Id(userId);
        emailTokenRepository.deleteByOwner_Id(userId);

        // remove collection knives then collection
        collectionRepository.findByUserId(userId).ifPresent(collection -> {
            collectionKnifeRepository.deleteAllByCollectionId(collection.getId());
            collectionRepository.delete(collection);
        });

        // remove posts
        postsRepository.deleteAllByAccountId(accountId);

        // remove account
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
