package com.example.BalisongFlipping.dtos.catalogSeedDtos;

import java.util.List;

public record KnifeSeedDto(
        String slug,
        String name,
        String maker,
        String makerSlug,
        String bladeStyle,
        String priceRange,
        List<VersionSeedDto> versions
) {}
