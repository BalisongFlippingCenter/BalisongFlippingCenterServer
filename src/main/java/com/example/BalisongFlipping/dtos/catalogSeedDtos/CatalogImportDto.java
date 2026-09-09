package com.example.BalisongFlipping.dtos.catalogSeedDtos;

import java.util.List;

// Body shape for POST /admin/catalog/import -- same per-item shape as the
// classpath seed-data/*.json files, just delivered over HTTP instead of
// requiring a deploy to change.
public record CatalogImportDto(
        List<MakerSeedDto> makers,
        List<KnifeSeedDto> knives
) {}
