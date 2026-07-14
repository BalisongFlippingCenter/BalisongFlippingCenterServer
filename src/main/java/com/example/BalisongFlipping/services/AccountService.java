package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.ConfirmEmailChangeDto;
import com.example.BalisongFlipping.dtos.ConfirmPasswordChangeDto;
import com.example.BalisongFlipping.dtos.DisplayNameChangeDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdatePreferencesDto;
import com.example.BalisongFlipping.dtos.UpdateSocialLinksDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.UserSearchResultDto;
import com.example.BalisongFlipping.modals.accounts.Account;

import java.util.List;

public interface AccountService {

    String generateIdentifierCode(String displayName);

    Account getAccount(String accountId) throws Exception;
    UserDto getSelf() throws Exception;
    Account getAccountByEmail(String userEmail) throws Exception;
    List<Account> allUsers();
    Boolean checkForAccountExistance(String accountId) throws Exception;
    void verifyAccountEmail(Account user) throws Exception;

    // public profile reads (no auth)
    PublicProfileDto getPublicProfileById(String accountId) throws Exception;
    PublicProfileDto getPublicProfileByHandle(String displayName, String identifierCode) throws Exception;
    List<UserSearchResultDto> searchUsers(String query);

    // profile
    DisplayNameChangeDto changeDisplayName(String accountId, String newDisplayName) throws Exception;
    UserDto updateBio(String accountId, String bio) throws Exception;
    String updateProfileImg(String accountId, String imageKey) throws Exception;
    String updateBannerImg(String accountId, String imageKey) throws Exception;
 
    // social links (consolidated)
    UserDto updateSocialLinks(String accountId, UpdateSocialLinksDto dto) throws Exception;

    // app preferences
    UserDto updatePreferences(String accountId, UpdatePreferencesDto dto) throws Exception;

    // google auth
    UserDto setInitialDisplayName(String accountId, String displayName) throws Exception;

    // follow / unfollow
    UserDto followAccount(String followerId, String targetId) throws Exception;
    UserDto unfollowAccount(String followerId, String targetId) throws Exception;
    List<UserSearchResultDto> getFollowing(String accountId) throws Exception;
    List<UserSearchResultDto> getFollowers(String accountId) throws Exception;
    boolean isFollowing(String followerId, String targetId);

    // post count maintenance
    void incrementPostCount(String accountId) throws Exception;
    void decrementPostCount(String accountId) throws Exception;

    // email / password change (2-step via email code)
    void requestEmailChange(String accountId) throws Exception;
    UserDto confirmEmailChange(String accountId, ConfirmEmailChangeDto dto) throws Exception;
    void requestPasswordChange(String accountId) throws Exception;
    void confirmPasswordChange(String accountId, ConfirmPasswordChangeDto dto) throws Exception;

    // danger zone
    UserDto hideAccount(String accountId) throws Exception;
    UserDto resetAccount(String accountId) throws Exception;
    void deleteAccount(String accountId) throws Exception;
}
