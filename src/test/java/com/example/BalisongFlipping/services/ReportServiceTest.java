package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.reportDtos.AdminReportDto;
import com.example.BalisongFlipping.dtos.reportDtos.CreateReportDto;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.reports.Report;
import com.example.BalisongFlipping.repositories.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ModerationService moderationService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        ReflectionTestUtils.setField(reportService, "reportRepository", reportRepository);
        ReflectionTestUtils.setField(reportService, "moderationService", moderationService);
    }

    @Test
    void submitReportSucceeds() throws Exception {
        when(reportRepository.existsByReporterAccountIdAndTargetTypeAndTargetId(1L, TargetType.POST, 5L)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report r = i.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(reportRepository.findById(10L)).thenAnswer(i -> Optional.empty());

        AdminReportDto result = reportService.submitReport(1L, new CreateReportDto("POST", 5L, "SPAM", "Spammy"));

        assertEquals(TargetType.POST, result.targetType());
        verify(moderationService).evaluate(any(Report.class));
    }

    @Test
    void submitReportRejectsInvalidTargetType() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("BOGUS", 5L, "SPAM", null)));
        assertEquals("Invalid targetType. Must be POST, COMMENT, or PROFILE.", ex.getMessage());
    }

    @Test
    void submitReportRejectsInvalidReason() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("POST", 5L, "BOGUS", null)));
        assertEquals("Invalid reason: BOGUS", ex.getMessage());
    }

    @Test
    void submitReportRejectsReasonNotValidForPost() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("POST", 5L, "INAPPROPRIATE_IMAGE", null)));
        assertTrue(ex.getMessage().contains("not valid for POST"));
    }

    @Test
    void submitReportRejectsReasonNotValidForComment() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("COMMENT", 5L, "ILLEGAL_LISTING", null)));
        assertTrue(ex.getMessage().contains("not valid for COMMENT"));
    }

    @Test
    void submitReportRejectsReasonNotValidForProfile() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("PROFILE", 5L, "SPAM", null)));
        assertTrue(ex.getMessage().contains("not valid for PROFILE"));
    }

    @Test
    void submitReportRejectsSelfReportOnProfile() {
        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("PROFILE", 1L, "HARASSMENT", null)));
        assertEquals("You cannot report your own profile.", ex.getMessage());
    }

    @Test
    void submitReportRejectsDuplicateReport() {
        when(reportRepository.existsByReporterAccountIdAndTargetTypeAndTargetId(1L, TargetType.POST, 5L)).thenReturn(true);

        Exception ex = assertThrows(Exception.class,
                () -> reportService.submitReport(1L, new CreateReportDto("POST", 5L, "SPAM", null)));
        assertEquals("You have already reported this post.", ex.getMessage());
        verify(reportRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getReports
    // -------------------------------------------------------------------------

    @Test
    void getReportsWithNoFiltersDelegatesToFindAll() throws Exception {
        when(reportRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reportService.getReports(null, null, 0, 20);

        verify(reportRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void getReportsWithStatusOnlyDelegatesToFindByStatus() throws Exception {
        when(reportRepository.findByStatus(eqStatus(ReportStatus.PENDING), any())).thenReturn(new PageImpl<>(List.of()));

        reportService.getReports("pending", null, 0, 20);

        verify(reportRepository).findByStatus(eqStatus(ReportStatus.PENDING), any());
    }

    @Test
    void getReportsWithTargetTypeOnlyDelegatesToFindByTargetType() throws Exception {
        when(reportRepository.findByTargetType(eqType(TargetType.POST), any())).thenReturn(new PageImpl<>(List.of()));

        reportService.getReports(null, "post", 0, 20);

        verify(reportRepository).findByTargetType(eqType(TargetType.POST), any());
    }

    @Test
    void getReportsWithBothFiltersDelegatesToCombinedQuery() throws Exception {
        when(reportRepository.findByStatusAndTargetType(eqStatus(ReportStatus.PENDING), eqType(TargetType.POST), any()))
                .thenReturn(new PageImpl<>(List.of()));

        reportService.getReports("pending", "post", 0, 20);

        verify(reportRepository).findByStatusAndTargetType(eqStatus(ReportStatus.PENDING), eqType(TargetType.POST), any());
    }

    @Test
    void getReportsRejectsInvalidStatus() {
        Exception ex = assertThrows(Exception.class, () -> reportService.getReports("bogus", null, 0, 20));
        assertEquals("Invalid status: bogus", ex.getMessage());
    }

    @Test
    void getReportsRejectsInvalidTargetType() {
        Exception ex = assertThrows(Exception.class, () -> reportService.getReports(null, "bogus", 0, 20));
        assertEquals("Invalid targetType: bogus", ex.getMessage());
    }

    private static ReportStatus eqStatus(ReportStatus status) {
        return org.mockito.ArgumentMatchers.eq(status);
    }

    private static TargetType eqType(TargetType type) {
        return org.mockito.ArgumentMatchers.eq(type);
    }

    // -------------------------------------------------------------------------
    // updateStatus
    // -------------------------------------------------------------------------

    @Test
    void updateStatusSucceeds() throws Exception {
        Report r = new Report();
        r.setId(10L);
        r.setTargetType(TargetType.POST);
        r.setTargetId(5L);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(r));
        when(reportRepository.save(r)).thenReturn(r);

        AdminReportDto result = reportService.updateStatus(10L, 99L, "REVIEWED");

        assertEquals(ReportStatus.REVIEWED, result.status());
        assertEquals(99L, r.getReviewedByAccountId());
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        Exception ex = assertThrows(Exception.class, () -> reportService.updateStatus(10L, 99L, "BOGUS"));
        assertTrue(ex.getMessage().startsWith("Invalid status."));
    }

    @Test
    void updateStatusRejectsUnknownReport() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());
        Exception ex = assertThrows(Exception.class, () -> reportService.updateStatus(999L, 99L, "REVIEWED"));
        assertEquals("Report not found.", ex.getMessage());
    }
}
