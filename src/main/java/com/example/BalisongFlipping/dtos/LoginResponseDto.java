package com.example.BalisongFlipping.dtos;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        Record account,
        CollectionDataDto collection
) {
}
