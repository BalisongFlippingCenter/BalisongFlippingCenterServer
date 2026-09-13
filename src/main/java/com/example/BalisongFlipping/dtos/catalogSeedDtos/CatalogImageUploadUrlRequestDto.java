package com.example.BalisongFlipping.dtos.catalogSeedDtos;

// versionSlug/variantSlug both present -> a variant image key.
// Both null/blank -> the knife's own cover photo key.
public record CatalogImageUploadUrlRequestDto(
        String knifeSlug,
        String versionSlug,
        String variantSlug,
        String filename,
        String contentType
) {}
