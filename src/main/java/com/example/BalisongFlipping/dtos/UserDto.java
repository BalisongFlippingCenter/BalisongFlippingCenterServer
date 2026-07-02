package com.example.BalisongFlipping.dtos;

import java.util.Set;

public record UserDto(
        String id,
        String email,
        Boolean emailVerified,
        String displayName,
        String identifierCode,
        String role,
        String collectionId,
        String bannerImg,
        String profileImg,
        String bio,
        String measurementUnit,
        String currency,
        boolean isHidden,
        String facebookLink,
        String twitterLink,
        String instagramLink,
        String youtubeLink,
        String discordLink,
        String redditLink,
        String personalEmailLink,
        String personalWebsiteLink,
        Set<Long> likedPostIds,
        Set<Long> likedCommentIds,
        int followerCount,
        int followingCount,
        int postCount
) {}
