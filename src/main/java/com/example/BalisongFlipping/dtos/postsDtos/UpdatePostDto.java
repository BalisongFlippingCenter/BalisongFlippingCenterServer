package com.example.BalisongFlipping.dtos.postsDtos;

public record UpdatePostDto(
        String caption,
        String description,
        Boolean isPrivate,
        String referenceKnifeId,
        String fileMetadata
) {}
