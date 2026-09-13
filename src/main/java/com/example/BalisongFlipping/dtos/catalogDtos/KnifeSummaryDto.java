package com.example.BalisongFlipping.dtos.catalogDtos;

public record KnifeSummaryDto(
        String slug,
        String name,
        String makerName,
        String makerSlug,
        String bladeStyleSummary,
        String handleMaterialSummary,
        String priceRangeSummary,
        String coverPhotoUrl,
        boolean hasActiveVersion
) {}
