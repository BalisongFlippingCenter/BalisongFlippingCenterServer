package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.ConfirmEmailChangeDto;
import com.example.BalisongFlipping.dtos.ConfirmPasswordChangeDto;
import com.example.BalisongFlipping.dtos.DisplayNameChangeDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdatePreferencesDto;
import com.example.BalisongFlipping.dtos.UpdateSocialLinksDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.UserSearchResultDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.example.BalisongFlipping.services.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private UserDto selfDto() {
        return new UserDto("1", "flipper@example.com", true, "Flipper", "0001", "USER",
                null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                Set.of(), Set.of(), Set.of(), 0, 0, 0);
    }

    private PublicProfileDto publicProfile() {
        return new PublicProfileDto("1", "Flipper", "0001", null, null, null, "5",
                false, null, null, null, null, null, null, null, null, 0, 0, 0);
    }

    @Test
    void searchUsersReturnsMatches() throws Exception {
        when(accountService.searchUsers("flip")).thenReturn(
                List.of(new UserSearchResultDto("1", "Flipper", "0001", null, null)));

        mockMvc.perform(get("/accounts/any/search").param("q", "flip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Flipper"));
    }

    @Test
    void searchUsersRejectsServiceFailure() throws Exception {
        when(accountService.searchUsers("flip")).thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(get("/accounts/any/search").param("q", "flip"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Search failed"));
    }

    @Test
    void getPublicProfileByIdSucceeds() throws Exception {
        when(accountService.getPublicProfileById("1")).thenReturn(publicProfile());

        mockMvc.perform(get("/accounts/any/{accountId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Flipper"));
    }

    @Test
    void getPublicProfileByIdReturnsNotFoundForUnknownAccount() throws Exception {
        when(accountService.getPublicProfileById("999")).thenThrow(new Exception("Account not found."));

        mockMvc.perform(get("/accounts/any/{accountId}", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found."));
    }

    @Test
    void getPublicProfileByHandleSucceeds() throws Exception {
        when(accountService.getPublicProfileByHandle("Flipper", "0001")).thenReturn(publicProfile());

        mockMvc.perform(get("/accounts/any")
                        .param("displayName", "Flipper")
                        .param("identifierCode", "0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Flipper"));
    }

    @Test
    void getMeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flipper@example.com"));
    }

    @Test
    void getMeReturnsNotFoundWhenUnauthenticated() throws Exception {
        when(accountService.getSelf()).thenThrow(new Exception("Not authenticated."));

        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not authenticated."));
    }

    @Test
    void changeDisplayNameSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.changeDisplayName(eq("1"), eq("NewName")))
                .thenReturn(new DisplayNameChangeDto("NewName", "0002"));

        mockMvc.perform(post("/accounts/me/change-display-name")
                        .contentType("text/plain")
                        .content("NewName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));
    }

    @Test
    void changeDisplayNameRejectsServiceFailure() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.changeDisplayName(eq("1"), anyString()))
                .thenThrow(new Exception("Display name recently changed."));

        mockMvc.perform(post("/accounts/me/change-display-name")
                        .contentType("text/plain")
                        .content("NewName"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Display name recently changed."));
    }

    @Test
    void updateBioSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.updateBio(eq("1"), eq("New bio"))).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/update-bio")
                        .contentType("text/plain")
                        .content("New bio"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfileImgSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.updateProfileImg(eq("1"), anyString())).thenReturn("https://cdn/profile.png");

        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/accounts/me/update-profile-img").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("https://cdn/profile.png"));
    }

    @Test
    void updateBannerImgSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.updateBannerImg(eq("1"), anyString())).thenReturn("https://cdn/banner.png");

        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/accounts/me/update-banner-img").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("https://cdn/banner.png"));
    }

    @Test
    void updateSocialLinksSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.updateSocialLinks(eq("1"), any())).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/update-social-links")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateSocialLinksDto(null, null, null, null, null, null, null, null))))
                .andExpect(status().isOk());
    }

    @Test
    void updatePreferencesSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.updatePreferences(eq("1"), any())).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/update-preferences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdatePreferencesDto("imperial", "USD"))))
                .andExpect(status().isOk());
    }

    @Test
    void getFollowingReturnsResults() throws Exception {
        when(accountService.getFollowing("1")).thenReturn(
                List.of(new UserSearchResultDto("2", "OtherFlipper", "0002", null, null)));

        mockMvc.perform(get("/accounts/any/{accountId}/following", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("OtherFlipper"));
    }

    @Test
    void getFollowersReturnsResults() throws Exception {
        when(accountService.getFollowers("1")).thenReturn(
                List.of(new UserSearchResultDto("2", "OtherFlipper", "0002", null, null)));

        mockMvc.perform(get("/accounts/any/{accountId}/followers", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("OtherFlipper"));
    }

    @Test
    void checkIsFollowingReturnsTrueWhenFollowing() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.isFollowing("1", "2")).thenReturn(true);

        mockMvc.perform(get("/accounts/any/{targetId}/follow", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void checkIsFollowingReturnsFalseWhenUnauthenticated() throws Exception {
        when(accountService.getSelf()).thenThrow(new Exception("Not authenticated."));

        mockMvc.perform(get("/accounts/any/{targetId}/follow", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));
    }

    @Test
    void followAccountSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.followAccount("1", "2")).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/any/{targetId}/follow", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void unfollowAccountSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.unfollowAccount("1", "2")).thenReturn(selfDto());

        mockMvc.perform(delete("/accounts/any/{targetId}/follow", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void requestEmailChangeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/request-email-change"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent to your current email."));
    }

    @Test
    void confirmEmailChangeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.confirmEmailChange(eq("1"), any())).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/confirm-email-change")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeDto("123456", "new@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void confirmEmailChangeRejectsBadCode() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.confirmEmailChange(eq("1"), any())).thenThrow(new Exception("Invalid or expired code."));

        mockMvc.perform(post("/accounts/me/confirm-email-change")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeDto("000000", "new@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Invalid or expired code."));
    }

    @Test
    void requestPasswordChangeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/request-password-change"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent to your email."));
    }

    @Test
    void confirmPasswordChangeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/confirm-password-change")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ConfirmPasswordChangeDto("123456", "new-password"))))
                .andExpect(status().isOk())
                .andExpect(content().string("Password updated successfully."));
    }

    @Test
    void confirmPasswordChangeRejectsBadCode() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        org.mockito.Mockito.doThrow(new Exception("Invalid or expired code."))
                .when(accountService).confirmPasswordChange(eq("1"), any());

        mockMvc.perform(post("/accounts/me/confirm-password-change")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ConfirmPasswordChangeDto("000000", "new-password"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Invalid or expired code."));
    }

    @Test
    void hideAccountSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.hideAccount("1")).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/hide-account"))
                .andExpect(status().isOk());
    }

    @Test
    void resetAccountSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(accountService.resetAccount("1")).thenReturn(selfDto());

        mockMvc.perform(post("/accounts/me/reset-account"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAccountSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(delete("/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted."));
    }

    @Test
    void deleteAccountRejectsServiceFailure() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        org.mockito.Mockito.doThrow(new Exception("Cannot delete an admin account."))
                .when(accountService).deleteAccount("1");

        mockMvc.perform(delete("/accounts/me"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Cannot delete an admin account."));
    }
}
