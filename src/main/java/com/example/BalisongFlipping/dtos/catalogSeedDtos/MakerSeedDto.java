package com.example.BalisongFlipping.dtos.catalogSeedDtos;

public record MakerSeedDto(
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
        String twitterUrl
) {}
