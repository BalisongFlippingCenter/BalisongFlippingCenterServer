package com.example.BalisongFlipping.dtos;

public record UpdateKnifeDto(
        // Required info
        String displayName,
        String knifeMaker,
        String baseKnifeModel,
        String knifeType,
        String aqquiredDate,
        boolean isFavoriteKnife,
        boolean isFavoriteFlipper,

        // Specs
        double knifeMSRP,
        double overallLength,
        double weight,
        String pivotSystem,
        String latchType,
        String pinSystem,
        boolean hasModularBalance,
        String balanceValue,

        // Blade
        String bladeStyle,
        String bladeFinish,
        String bladeMaterial,

        // Handles
        String handleConstruction,
        String handleMaterial,
        String handleFinish,

        // Scores (backend recalculates average)
        double qualityScore,
        double flippingScore,
        double feelScore,
        double soundScore,
        double durabilityScore
) {}
