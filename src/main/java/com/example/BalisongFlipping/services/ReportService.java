package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.reportDtos.AdminReportDto;
import com.example.BalisongFlipping.dtos.reportDtos.CreateReportDto;
import com.example.BalisongFlipping.enums.reports.ReportReason;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.reports.Report;
import com.example.BalisongFlipping.repositories.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
public class ReportService {

    private static final Set<ReportReason> POST_COMMENT_REASONS = EnumSet.of(
            ReportReason.SPAM, ReportReason.ILLEGAL_LISTING, ReportReason.INAPPROPRIATE_CONTENT,
            ReportReason.HARASSMENT, ReportReason.MISINFORMATION, ReportReason.OTHER);

    private static final Set<ReportReason> PROFILE_REASONS = EnumSet.of(
            ReportReason.INAPPROPRIATE_IMAGE, ReportReason.INAPPROPRIATE_NAME,
            ReportReason.INAPPROPRIATE_BIO, ReportReason.HARASSMENT, ReportReason.OTHER);

    @Autowired private ReportRepository reportRepository;

    @Transactional
    public AdminReportDto submitReport(Long reporterAccountId, CreateReportDto dto) throws Exception {
        TargetType targetType;
        try {
            targetType = TargetType.valueOf(dto.targetType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid targetType. Must be POST, COMMENT, or PROFILE.");
        }

        ReportReason reason;
        try {
            reason = ReportReason.valueOf(dto.reason().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid reason: " + dto.reason());
        }

        // Validate reason is valid for target type
        if (targetType == TargetType.PROFILE && !PROFILE_REASONS.contains(reason))
            throw new Exception("Reason " + reason + " is not valid for PROFILE reports.");
        if ((targetType == TargetType.POST || targetType == TargetType.COMMENT) && !POST_COMMENT_REASONS.contains(reason))
            throw new Exception("Reason " + reason + " is not valid for " + targetType + " reports.");

        // Prevent self-reporting
        if (targetType == TargetType.PROFILE && dto.targetId().equals(reporterAccountId))
            throw new Exception("You cannot report your own profile.");

        // Prevent duplicate reports
        if (reportRepository.existsByReporterAccountIdAndTargetTypeAndTargetId(reporterAccountId, targetType, dto.targetId()))
            throw new Exception("You have already reported this " + targetType.name().toLowerCase() + ".");

        Report report = new Report();
        report.setReporterAccountId(reporterAccountId);
        report.setTargetType(targetType);
        report.setTargetId(dto.targetId());
        report.setReason(reason);
        report.setAdditionalNote(dto.additionalNote());

        return toAdminDto(reportRepository.save(report));
    }

    public Page<AdminReportDto> getReports(String status, String targetType, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        ReportStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try { parsedStatus = ReportStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new Exception("Invalid status: " + status); }
        }

        TargetType parsedType = null;
        if (targetType != null && !targetType.isBlank()) {
            try { parsedType = TargetType.valueOf(targetType.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new Exception("Invalid targetType: " + targetType); }
        }

        Page<Report> results;
        if (parsedStatus != null && parsedType != null)
            results = reportRepository.findByStatusAndTargetType(parsedStatus, parsedType, pageable);
        else if (parsedStatus != null)
            results = reportRepository.findByStatus(parsedStatus, pageable);
        else if (parsedType != null)
            results = reportRepository.findByTargetType(parsedType, pageable);
        else
            results = reportRepository.findAll(pageable);

        return results.map(this::toAdminDto);
    }

    @Transactional
    public AdminReportDto updateStatus(Long reportId, Long adminAccountId, String status) throws Exception {
        ReportStatus newStatus;
        try {
            newStatus = ReportStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid status. Must be PENDING, REVIEWED, DISMISSED, or ACTIONED.");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new Exception("Report not found."));

        report.setStatus(newStatus);
        report.setReviewedAt(Instant.now());
        report.setReviewedByAccountId(adminAccountId);

        return toAdminDto(reportRepository.save(report));
    }

    private AdminReportDto toAdminDto(Report r) {
        return new AdminReportDto(
                r.getId(),
                r.getTargetType(),
                r.getTargetId(),
                r.getReason(),
                r.getAdditionalNote(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getReviewedAt(),
                r.getReviewedByAccountId()
        );
    }
}
