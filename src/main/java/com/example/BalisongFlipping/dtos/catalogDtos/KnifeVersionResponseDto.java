package com.example.BalisongFlipping.dtos.catalogDtos;

import java.util.List;

public record KnifeVersionResponseDto(
        String versionSlug,
        String versionLabel,
        boolean discontinued,
        Integer releaseYear,
        String description,
        Double overallLength,
        Double weight,
        String pivotSystem,
        String latchType,
        String pinSystem,
        boolean hasModularBalance,
        String balanceValue,
        String handleConstruction,
        String handleMaterial,
        String handleFinish,
        List<KnifeVariantResponseDto> variants,
        List<WhereToFindResponseDto> whereToFind
) {}
