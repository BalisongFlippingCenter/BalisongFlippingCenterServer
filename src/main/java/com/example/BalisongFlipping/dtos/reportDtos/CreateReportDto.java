package com.example.BalisongFlipping.dtos.reportDtos;

public record CreateReportDto(
        String targetType,
        Long targetId,
        String reason,
        String additionalNote
) {}
