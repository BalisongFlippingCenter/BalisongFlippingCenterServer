package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.AuthService;
import com.example.BalisongFlipping.services.CollectionService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private AuthService authenticationService;

    @MockBean
    private CollectionService collectionService;

    @MockBean
    private AccountService accountService;

    private User verifiedUser() {
        User user = new User();
        user.setEmail("flipper@example.com");
        user.setRole("USER");
        user.setEmailVerified(true);
        user.setId(1L);
        return user;
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginAccountDto(email, password));
    }

    @Test
    void loginSucceedsForVerifiedUser() throws Exception {
        User user = verifiedUser();

        when(authenticationService.authenticate(any())).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-123");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-456");
        refreshToken.setOwner(user);
        when(refreshTokenService.createRefreshToken("flipper@example.com")).thenReturn(refreshToken);

        when(collectionService.getCollection(any())).thenReturn(null);
        when(accountService.toUserDto(user)).thenReturn(new UserDto(
                "1", "flipper@example.com", true, "Flipper", "0001", "USER",
                null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of(),
                0, 0, 0
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "correct-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Refresh-Token-Cookie=refresh-token-456")));
    }

    @Test
    void loginRequiresSecondFactorForAdmin() throws Exception {
        User admin = verifiedUser();
        admin.setRole("ADMIN");

        when(authenticationService.authenticate(any())).thenReturn(admin);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "correct-password")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requiresAdminVerification").value(true))
                .andExpect(jsonPath("$.email").value("flipper@example.com"));

        verify(authenticationService).sendAdminLoginCode(admin);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void loginRejectsUnverifiedEmail() throws Exception {
        User user = verifiedUser();
        user.setEmailVerified(false);

        when(authenticationService.authenticate(any())).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "correct-password")))
                .andExpect(status().isConflict())
                .andExpect(content().string("Please verify your email before logging in."));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void loginRejectsBannedAccount() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(new DisabledException("This account has been banned."));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "correct-password")))
                .andExpect(status().isConflict())
                .andExpect(content().string("This account has been banned."));
    }

    @Test
    void loginRejectsSuspendedAccount() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(new LockedException("This account is suspended until 2026-01-01."));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "correct-password")))
                .andExpect(status().isConflict())
                .andExpect(content().string("This account is suspended until 2026-01-01."));
    }

    @Test
    void loginRejectsBadCredentialsWithGenericFailureMessage() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody("flipper@example.com", "wrong-password")))
                .andExpect(status().isConflict())
                .andExpect(content().string("Failed: Invalid credentials"));
    }
}
