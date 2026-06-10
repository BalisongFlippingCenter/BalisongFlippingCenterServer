package com.example.BalisongFlipping.dtos;

public record CollectionProfileDto(
        String accountId,
        String displayName,
        String identifierCode,
        String profileImg,
        CollectionDataDto collection
) {}
