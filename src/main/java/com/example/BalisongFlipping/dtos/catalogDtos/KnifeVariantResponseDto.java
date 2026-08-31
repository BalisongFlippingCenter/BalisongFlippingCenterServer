package com.example.BalisongFlipping.dtos.catalogDtos;

public record KnifeVariantResponseDto(
        String variantSlug,
        String type,
        String label,
        Double msrp,
        String bladeStyle,
        String bladeMaterial,
        String bladeFinish
) {}
