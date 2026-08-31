package com.example.BalisongFlipping.dtos.catalogDtos;

import java.util.List;

public record MakerDetailDto(
        String slug,
        String name,
        String country,
        String knownFor,
        String officialSiteUrl,
        List<KnifeSummaryDto> knives
) {}
