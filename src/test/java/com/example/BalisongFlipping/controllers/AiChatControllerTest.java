package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.AiChatService;
import com.example.BalisongFlipping.services.AiClientAuthService;
import com.example.BalisongFlipping.services.AiRateLimiterService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiClientAuthService aiClientAuthService;

    @MockBean
    private AiRateLimiterService aiRateLimiterService;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void clearSessionClearsTheCallingClientsOwnSession() throws Exception {
        when(aiClientAuthService.isValid("discord", "key")).thenReturn(true);
        when(aiChatService.clearSession("discord-1-2")).thenReturn(true);

        mockMvc.perform(delete("/ai/chat/session/discord-1-2")
                        .header("X-Client-Id", "discord")
                        .header("X-Client-Key", "key"))
                .andExpect(status().isNoContent());

        verify(aiChatService).clearSession("discord-1-2");
    }

    @Test
    void clearSessionRejectsInvalidClientCredentials() throws Exception {
        when(aiClientAuthService.isValid("discord", "wrong")).thenReturn(false);

        mockMvc.perform(delete("/ai/chat/session/discord-1-2")
                        .header("X-Client-Id", "discord")
                        .header("X-Client-Key", "wrong"))
                .andExpect(status().isUnauthorized());

        verify(aiChatService, never()).clearSession(anyString());
    }

    @Test
    void clearSessionRejectsAnotherClientsSession() throws Exception {
        when(aiClientAuthService.isValid("website", "key")).thenReturn(true);

        mockMvc.perform(delete("/ai/chat/session/discord-1-2")
                        .header("X-Client-Id", "website")
                        .header("X-Client-Key", "key"))
                .andExpect(status().isForbidden());

        verify(aiChatService, never()).clearSession(anyString());
    }

    @Test
    void clearSessionReturnsBadGatewayWhenTheAiServiceFails() throws Exception {
        when(aiClientAuthService.isValid("discord", "key")).thenReturn(true);
        when(aiChatService.clearSession("discord-1-2")).thenReturn(false);

        mockMvc.perform(delete("/ai/chat/session/discord-1-2")
                        .header("X-Client-Id", "discord")
                        .header("X-Client-Key", "key"))
                .andExpect(status().isBadGateway());
    }
}
