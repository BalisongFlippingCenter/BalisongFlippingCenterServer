package com.example.BalisongFlipping.dtos.postsDtos;

import com.example.BalisongFlipping.enums.knives.*;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;

public record PostKnifeDto(
        Long id,
        String displayName,
        String knifeMaker,
        String baseKnifeModel,
        String coverPhoto,
        KnifeType knifeType,
        BladeStyle bladeStyle,
        BladeFinish bladeFinish,
        BladeMaterial bladeMaterial,
        HandleConstruction handleConstruction,
        HandleFinish handleFinish,
        HandleMaterial handleMaterial,
        PivotSystem pivotSystem,
        PinSystem pinSystem,
        LatchType latchType,
        boolean hasModularBalance,
        String balanceValue,
        double overallLength,
        double weight,
        double msrp,
        double averageScore,
        double qualityScore,
        double flippingScore,
        double feelScore,
        double soundScore,
        double durabilityScore
) {
    public static PostKnifeDto from(CollectionKnife k) {
        return new PostKnifeDto(
                k.getId(),
                k.getDisplayName(),
                k.getKnifeMaker(),
                k.getBaseKnifeModel(),
                k.getCoverPhoto(),
                k.getKnifeType(),
                k.getBladeStyle(),
                k.getBladeFinish(),
                k.getBladeMaterial(),
                k.getHandleConstruction(),
                k.getHandleFinish(),
                k.getHandleMaterial(),
                k.getPivotSystem(),
                k.getPinSystem(),
                k.getLatchType(),
                k.isHasModularBalance(),
                k.getBalanceValue(),
                k.getOverallLength(),
                k.getWeight(),
                k.getMsrp(),
                k.getAverageScore(),
                k.getQualityScore(),
                k.getFlippingScore(),
                k.getFeelScore(),
                k.getSoundScore(),
                k.getDurabilityScore()
        );
    }
}
