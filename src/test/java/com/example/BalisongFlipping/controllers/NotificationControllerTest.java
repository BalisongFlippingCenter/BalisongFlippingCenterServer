package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.notificationDtos.NotificationDto;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.NotificationService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void authenticateAsUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("flipper@example.com");
        user.setRole("USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private NotificationDto notificationDto(Long id, boolean isRead) {
        return new NotificationDto(id, NotificationType.POST_LIKED, "Someone liked your post",
                TargetType.POST, 5L, "Flipper", "0001", null, isRead,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void getNotificationsReturnsPagedResults() throws Exception {
        Page<NotificationDto> page = new PageImpl<>(List.of(notificationDto(1L, false)));
        when(notificationService.getNotifications(eq(1L), eq(false), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getNotificationsRejectsServiceFailure() throws Exception {
        when(notificationService.getNotifications(eq(1L), eq(false), eq(0), eq(20)))
                .thenThrow(new RuntimeException("Something broke"));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Something broke"));
    }

    @Test
    void getUnreadCountReturnsCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(7L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void markReadSucceeds() throws Exception {
        when(notificationService.markRead(5L, 1L)).thenReturn(notificationDto(5L, true));

        mockMvc.perform(patch("/notifications/{id}/read", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markReadRejectsUnknownNotification() throws Exception {
        when(notificationService.markRead(999L, 1L)).thenThrow(new Exception("Notification not found."));

        mockMvc.perform(patch("/notifications/{id}/read", 999))
                .andExpect(status().isConflict())
                .andExpect(content().string("Notification not found."));
    }

    @Test
    void markAllReadSucceeds() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read."));

        verify(notificationService).markAllRead(1L);
    }
}
