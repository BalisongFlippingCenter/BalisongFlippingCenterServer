package com.example.BalisongFlipping.dtos;

public record UpdateSocialLinksDto(
        String facebookLink,
        String twitterLink,
        String instagramLink,
        String youtubeLink,
        String discordLink,
        String redditLink,
        String personalEmailLink,
        String personalWebsiteLink
) {}
