package com.example.BalisongFlipping.dtos.catalogSeedDtos;

import java.util.List;

public record VersionSeedDto(
        String versionSlug,
        String version,
        boolean discontinued,
        Integer releaseYear,
        String description,
        String overallLength,
        String weight,
        String pivotSystem,
        String latchType,
        String pinSystem,
        boolean hasModularBalance,
        String balanceValue,
        String handleConstruction,
        String handleMaterial,
        String handleFinish,
        List<VariantSeedDto> variants,
        List<WhereToFindSeedDto> whereToFind
) {}
