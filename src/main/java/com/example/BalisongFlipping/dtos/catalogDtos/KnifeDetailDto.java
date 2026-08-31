package com.example.BalisongFlipping.dtos.catalogDtos;

import java.util.List;

public record KnifeDetailDto(
        String slug,
        String name,
        String makerName,
        String makerSlug,
        String bladeStyleSummary,
        String priceRangeSummary,
        List<KnifeVersionResponseDto> versions
) {}
