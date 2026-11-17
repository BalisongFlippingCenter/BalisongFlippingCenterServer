package com.example.BalisongFlipping.dtos;

public record GoogleLoginResponseDto(
        String accessToken,
        Record account,
        CollectionDataDto collection,
        boolean isNewUser
) {}
