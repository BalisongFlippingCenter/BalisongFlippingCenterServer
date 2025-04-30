package com.example.BalisongFlipping.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.BalisongFlipping.dtos.DisplayNameChangeDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.modals.accounts.Account;


public interface AccountService {

    String generateIdentifierCode(
            final String displayName);

    Account getAccount(
            final String accountId) throws Exception;

    UserDto getSelf() throws Exception;

    List<Account> allUsers();

    Boolean checkForAccountExistance(final String accountId) throws Exception;

    // methods to update social media links
    String updateFacebookLink(final String newLink, final String accountId) throws Exception;
    String updateInstagramLink(final String newLink, final String accountId) throws Exception;
    String updateTwitterLink(final String newLink, final String accountId) throws Exception;
    String updateYoutubeLink(final String newLink, final String accountId) throws Exception;
    String updateRedditLink(final String newLink, final String accountId) throws Exception;
    String updateDiscordLink(final String newLink, final String accountId) throws Exception;
    String updatePersonalEmailLink(final String newLink, final String accountId)
            throws Exception;
    String updatePersonalWebsiteLink(final String newLink, final String accountId)
            throws Exception;

    DisplayNameChangeDto changeDisplayName(final String accountID, final String newDisplayName) throws Exception;

    String updateProfileImg(final String accountId, final MultipartFile file);

    String updateBannerImg(final String accountId, final MultipartFile file);
}