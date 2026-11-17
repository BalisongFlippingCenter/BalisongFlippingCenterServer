package com.example.BalisongFlipping.dtos;

public record LoginResponseDto(
        String accessToken,
        Record account,
        CollectionDataDto collection
) {
}
