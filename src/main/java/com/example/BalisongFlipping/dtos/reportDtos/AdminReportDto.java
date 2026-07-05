package com.example.BalisongFlipping.dtos.reportDtos;

import com.example.BalisongFlipping.enums.reports.ReportReason;
import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;

import java.time.Instant;

// Reporter identity intentionally excluded to prevent admin bias
public record AdminReportDto(
        Long id,
        TargetType targetType,
        Long targetId,
        ReportReason reason,
        String additionalNote,
        ReportStatus status,
        Instant createdAt,
        Instant reviewedAt,
        Long reviewedByAccountId
) {}
