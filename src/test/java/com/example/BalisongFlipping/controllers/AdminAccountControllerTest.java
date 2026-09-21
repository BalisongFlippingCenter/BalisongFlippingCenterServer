package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.AdminAccountSummaryDto;
import com.example.BalisongFlipping.dtos.BanAccountDto;
import com.example.BalisongFlipping.dtos.MuteAccountDto;
import com.example.BalisongFlipping.dtos.SuspendAccountDto;
import com.example.BalisongFlipping.services.AdminAccountService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAccountService adminAccountService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private AdminAccountSummaryDto summary(boolean banned, String banReason) {
        return new AdminAccountSummaryDto("1", "flipper@example.com", "Flipper", "0001", "USER",
                null, banned, banReason, null, null, null, null);
    }

    @Test
    void searchReturnsMatches() throws Exception {
        when(adminAccountService.search("flip")).thenReturn(List.of(summary(false, null)));

        mockMvc.perform(get("/admin/accounts/search").param("q", "flip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("flipper@example.com"));
    }

    @Test
    void getByIdReturnsAccount() throws Exception {
        when(adminAccountService.getById("1")).thenReturn(summary(false, null));

        mockMvc.perform(get("/admin/accounts/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flipper@example.com"));
    }

    @Test
    void getByIdReturnsNotFoundForUnknownAccount() throws Exception {
        when(adminAccountService.getById("999")).thenThrow(new Exception("Account not found."));

        mockMvc.perform(get("/admin/accounts/{id}", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found."));
    }

    @Test
    void banSucceeds() throws Exception {
        when(adminAccountService.ban(eq("1"), eq("Repeated spam"))).thenReturn(summary(true, "Repeated spam"));

        mockMvc.perform(post("/admin/accounts/{id}/ban", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BanAccountDto("Repeated spam"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true));
    }

    @Test
    void banRejectsServiceFailure() throws Exception {
        when(adminAccountService.ban(eq("999"), eq("Repeated spam")))
                .thenThrow(new Exception("Account not found."));

        mockMvc.perform(post("/admin/accounts/{id}/ban", "999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BanAccountDto("Repeated spam"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Account not found."));
    }

    @Test
    void unbanSucceeds() throws Exception {
        when(adminAccountService.unban("1")).thenReturn(summary(false, null));

        mockMvc.perform(post("/admin/accounts/{id}/unban", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void suspendSucceeds() throws Exception {
        when(adminAccountService.suspend(eq("1"), eq("Cooldown"), eq("2026-09-22T00:00:00Z")))
                .thenReturn(summary(false, null));

        mockMvc.perform(post("/admin/accounts/{id}/suspend", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SuspendAccountDto("Cooldown", "2026-09-22T00:00:00Z"))))
                .andExpect(status().isOk());
    }

    @Test
    void unsuspendSucceeds() throws Exception {
        when(adminAccountService.unsuspend("1")).thenReturn(summary(false, null));

        mockMvc.perform(post("/admin/accounts/{id}/unsuspend", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void muteSucceeds() throws Exception {
        when(adminAccountService.mute(eq("1"), eq("Harassment"), eq("2026-09-22T00:00:00Z")))
                .thenReturn(summary(false, null));

        mockMvc.perform(post("/admin/accounts/{id}/mute", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new MuteAccountDto("Harassment", "2026-09-22T00:00:00Z"))))
                .andExpect(status().isOk());
    }

    @Test
    void unmuteSucceeds() throws Exception {
        when(adminAccountService.unmute("1")).thenReturn(summary(false, null));

        mockMvc.perform(post("/admin/accounts/{id}/unmute", "1"))
                .andExpect(status().isOk());
    }
}
