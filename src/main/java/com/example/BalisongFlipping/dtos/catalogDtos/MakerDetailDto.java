package com.example.BalisongFlipping.dtos.catalogDtos;

import java.util.List;

public record MakerDetailDto(
        String slug,
        String name,
        String country,
        String knownFor,
        String officialSiteUrl,
        String logoUrl,
        Integer foundedYear,
        String instagramUrl,
        String youtubeUrl,
        String facebookUrl,
        String twitterUrl,
        List<KnifeSummaryDto> knives
) {}
