package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.reportDtos.AdminReportDto;
import com.example.BalisongFlipping.dtos.reportDtos.CreateReportDto;
import com.example.BalisongFlipping.dtos.reportDtos.UpdateReportStatusDto;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.example.BalisongFlipping.services.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

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

    private AdminReportDto reportDto(Long id, ReportStatus status) {
        return new AdminReportDto(id, TargetType.POST, 42L, null, "Spam listing", status,
                Instant.parse("2026-01-01T00:00:00Z"), null, null);
    }

    @Test
    void submitReportSucceeds() throws Exception {
        when(reportService.submitReport(eq(1L), any())).thenReturn(reportDto(10L, ReportStatus.PENDING));

        mockMvc.perform(post("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateReportDto("POST", 42L, "SPAM", "Spam listing"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitReportRejectsInvalidTargetType() throws Exception {
        when(reportService.submitReport(eq(1L), any()))
                .thenThrow(new Exception("Invalid targetType. Must be POST, COMMENT, or PROFILE."));

        mockMvc.perform(post("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateReportDto("BOGUS", 42L, "SPAM", null))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Invalid targetType. Must be POST, COMMENT, or PROFILE."));
    }

    @Test
    void getReportsReturnsPagedResults() throws Exception {
        Page<AdminReportDto> page = new PageImpl<>(List.of(reportDto(1L, ReportStatus.PENDING)));
        when(reportService.getReports(isNull(), isNull(), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getReportsRejectsInvalidStatus() throws Exception {
        when(reportService.getReports(eq("bogus"), isNull(), anyInt(), anyInt()))
                .thenThrow(new Exception("Invalid status: bogus"));

        mockMvc.perform(get("/reports").param("status", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid status: bogus"));
    }

    @Test
    void updateStatusSucceeds() throws Exception {
        when(reportService.updateStatus(eq(10L), eq(1L), anyString()))
                .thenReturn(reportDto(10L, ReportStatus.REVIEWED));

        mockMvc.perform(patch("/reports/{id}/status", 10)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateReportStatusDto("REVIEWED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));
    }

    @Test
    void updateStatusRejectsUnknownReport() throws Exception {
        when(reportService.updateStatus(eq(999L), eq(1L), anyString()))
                .thenThrow(new Exception("Report not found."));

        mockMvc.perform(patch("/reports/{id}/status", 999)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateReportStatusDto("REVIEWED"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Report not found."));
    }
}
