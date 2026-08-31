package com.example.BalisongFlipping.dtos.catalogSeedDtos;

public record VariantSeedDto(
        String variantSlug,
        String type,
        String label,
        String msrp,
        String bladeStyle,
        String bladeMaterial,
        String bladeFinish
) {}
