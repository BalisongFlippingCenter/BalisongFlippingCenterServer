package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.enums.reports.ReportReason;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.reports.Report;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private AccountService accountService;

    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService();
        ReflectionTestUtils.setField(moderationService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(moderationService, "reportRepository", reportRepository);
        ReflectionTestUtils.setField(moderationService, "notificationService", notificationService);
        ReflectionTestUtils.setField(moderationService, "emailService", emailService);
        ReflectionTestUtils.setField(moderationService, "accountService", accountService);
    }

    private User user(Long id, String displayName, String bio) {
        User u = new User();
        u.setId(id);
        u.setEmail("flipper" + id + "@example.com");
        u.setDisplayName(displayName);
        u.setBio(bio);
        return u;
    }

    private Report report(TargetType targetType, ReportReason reason, Long targetId) {
        Report r = new Report();
        r.setTargetType(targetType);
        r.setReason(reason);
        r.setTargetId(targetId);
        return r;
    }

    @Test
    void evaluateIgnoresNonProfileReports() {
        Report r = report(TargetType.POST, ReportReason.SPAM, 1L);

        moderationService.evaluate(r);

        verify(accountRepository, never()).findById(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void evaluateIgnoresProfileReportsWithUnrelatedReason() {
        Report r = report(TargetType.PROFILE, ReportReason.HARASSMENT, 1L);

        moderationService.evaluate(r);

        verify(reportRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Inappropriate name
    // -------------------------------------------------------------------------

    @Test
    void evaluateResetsProfaneDisplayNameAndNotifies() {
        User user = user(1L, "cuntflipper", null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountService.generateIdentifierCode(anyString())).thenReturn("0001");

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_NAME, 1L);
        moderationService.evaluate(r);

        assertNotEquals("cuntflipper", user.getDisplayName());
        assertEquals(ReportStatus.ACTIONED, r.getStatus());
        verify(accountRepository).save(user);
        verify(notificationService).sendSystem(eq(1L), any(), any(), eq(1L));
        verify(emailService).sendEmail(eq("flipper1@example.com"), anyString(), anyString());
        verify(reportRepository).save(r);
    }

    @Test
    void evaluateDismissesReportForCleanDisplayName() {
        User user = user(1L, "Flipper", null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user));

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_NAME, 1L);
        moderationService.evaluate(r);

        assertEquals(ReportStatus.DISMISSED, r.getStatus());
        assertEquals("Flipper", user.getDisplayName());
        verify(accountRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void evaluateDoesNothingWhenAccountNoLongerExists() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_NAME, 999L);
        moderationService.evaluate(r);

        verify(reportRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Inappropriate bio
    // -------------------------------------------------------------------------

    @Test
    void evaluateClearsProfaneBioAndNotifies() {
        User user = user(1L, "Flipper", "I love cuntflipping knives");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user));

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_BIO, 1L);
        moderationService.evaluate(r);

        assertEquals("", user.getBio());
        assertEquals(ReportStatus.ACTIONED, r.getStatus());
        verify(notificationService).sendSystem(eq(1L), any(), any(), eq(1L));
        verify(emailService).sendEmail(eq("flipper1@example.com"), anyString(), anyString());
    }

    @Test
    void evaluateDismissesReportForCleanBio() {
        User user = user(1L, "Flipper", "I love flipping knives");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user));

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_BIO, 1L);
        moderationService.evaluate(r);

        assertEquals(ReportStatus.DISMISSED, r.getStatus());
        assertEquals("I love flipping knives", user.getBio());
    }

    @Test
    void evaluateDismissesReportForNullBio() {
        User user = user(1L, "Flipper", null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user));

        Report r = report(TargetType.PROFILE, ReportReason.INAPPROPRIATE_BIO, 1L);
        moderationService.evaluate(r);

        assertEquals(ReportStatus.DISMISSED, r.getStatus());
    }
}
