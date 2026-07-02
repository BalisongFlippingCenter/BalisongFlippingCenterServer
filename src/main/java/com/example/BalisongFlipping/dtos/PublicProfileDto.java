package com.example.BalisongFlipping.dtos;

public record PublicProfileDto(
        String id,
        String displayName,
        String identifierCode,
        String profileImg,
        String bannerImg,
        String bio,
        String collectionId,
        boolean isHidden,
        String facebookLink,
        String twitterLink,
        String instagramLink,
        String youtubeLink,
        String discordLink,
        String redditLink,
        String personalEmailLink,
        String personalWebsiteLink,
        int followerCount,
        int followingCount,
        int postCount
) {}
