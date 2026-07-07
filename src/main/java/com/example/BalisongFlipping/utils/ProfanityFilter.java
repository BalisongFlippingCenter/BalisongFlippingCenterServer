package com.example.BalisongFlipping.utils;

import java.util.Set;

public class ProfanityFilter {

    // Always flagged as substring — severe slurs where no innocent word contains them
    private static final Set<String> HARD_BANNED = Set.of(
            "nigger", "nigga", "faggot", "chink", "spic", "kike", "tranny",
            "cunt", "motherfucker", "motherfucking", "cocksucker", "nonce",
            "retard", "retarded", "beaner", "wetback", "towelhead", "raghead",
            "gook", "cracker", "honky", "dyke", "pedo", "pedophile"
    );

    // Only flagged when they appear as whole tokens (split on non-alpha boundaries)
    // Avoids false positives like "class" → "ass", "bass", "grasshopper"
    private static final Set<String> WORD_BANNED = Set.of(
            "fuck", "fucking", "fucker", "fucked", "fucks",
            "shit", "shits", "shitting", "shitty",
            "bitch", "bitches", "bitchy",
            "dick", "dicks",
            "cock", "cocks",
            "pussy", "pussies",
            "whore", "whores",
            "slut", "sluts",
            "bastard", "bastards",
            "asshole", "assholes",
            "bullshit",
            "piss", "pissed",
            "twat", "twats",
            "ass", "asses",
            "damn", "damned",
            "crap", "crappy",
            "prick", "pricks",
            "skank", "skanky",
            "wank", "wanker", "wankers",
            "bollocks", "tosser",
            "cum", "cumshot",
            "jizz", "boner",
            "rape", "raping", "rapist",
            "kill", "killing", "murder", "terrorist", "jihad",
            "nazi", "hitler"
    );

    public static boolean containsProfanity(String input) {
        if (input == null || input.isEmpty()) return false;

        String normalized = normalize(input);

        // Hard banned — substring match anywhere
        for (String word : HARD_BANNED) {
            if (normalized.contains(word)) return true;
        }

        // Word banned — whole token match only
        String[] tokens = normalized.split("[^a-z]+");
        for (String token : tokens) {
            if (!token.isEmpty() && WORD_BANNED.contains(token)) return true;
        }

        return false;
    }

    private static String normalize(String input) {
        String lower = input.toLowerCase();

        // Leet speak substitution
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case '4': case '@': sb.append('a'); break;
                case '3':           sb.append('e'); break;
                case '1': case '!': sb.append('i'); break;
                case '0':           sb.append('o'); break;
                case '$': case '5': sb.append('s'); break;
                case '+':           sb.append('t'); break;
                case '7':           sb.append('t'); break;
                default:            sb.append(c);   break;
            }
        }

        // "ph" → "f" (e.g. "phuck")
        String s = sb.toString().replace("ph", "f");

        // Collapse 3+ repeated characters: "fuuuuck" → "fuck", "shiiit" → "shit"
        // Uses 3+ threshold so "ass" (2 s's) is preserved correctly
        s = s.replaceAll("(.)\\1{2,}", "$1");

        return s;
    }
}
