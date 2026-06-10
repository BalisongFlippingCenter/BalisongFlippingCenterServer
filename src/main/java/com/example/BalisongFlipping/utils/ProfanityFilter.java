package com.example.BalisongFlipping.utils;

import java.util.Set;

public class ProfanityFilter {

    private static final Set<String> BANNED_WORDS = Set.of(
            "fuck", "shit", "ass", "bitch", "cunt", "dick", "cock",
            "pussy", "nigger", "nigga", "faggot", "fag", "whore",
            "slut", "bastard", "damn", "crap", "piss", "twat",
            "asshole", "motherfucker", "fucker", "bullshit"
    );

    public static boolean containsProfanity(String input) {
        if (input == null || input.isEmpty()) return false;
        String lower = input.toLowerCase();
        for (String word : BANNED_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }
}
