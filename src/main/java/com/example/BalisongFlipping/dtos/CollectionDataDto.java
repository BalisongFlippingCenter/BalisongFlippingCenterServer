package com.example.BalisongFlipping.dtos;

import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;

import java.util.List;

public record CollectionDataDto(
        String id,
        String accountId,
        String bannerImage,
        String featuredKnifeId,
        List<CollectionKnife> collectedKnives
) {
}
