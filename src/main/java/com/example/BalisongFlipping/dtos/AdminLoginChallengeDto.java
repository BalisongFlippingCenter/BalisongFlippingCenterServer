package com.example.BalisongFlipping.dtos;

public record AdminLoginChallengeDto(
        boolean requiresAdminVerification,
        String email
) {}
