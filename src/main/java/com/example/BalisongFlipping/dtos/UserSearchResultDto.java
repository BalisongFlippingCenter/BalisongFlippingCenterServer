package com.example.BalisongFlipping.dtos;

public record UserSearchResultDto(
        String accountId,
        String displayName,
        String identifierCode,
        String profileImg,
        String profileCaption
) {}
