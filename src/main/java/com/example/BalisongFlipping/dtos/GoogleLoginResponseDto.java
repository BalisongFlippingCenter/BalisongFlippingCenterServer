package com.example.BalisongFlipping.dtos;

public record GoogleLoginResponseDto(
        String accessToken,
        String refreshToken,
        Record account,
        CollectionDataDto collection,
        boolean isNewUser
) {}
