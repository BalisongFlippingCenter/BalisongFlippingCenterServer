package com.example.BalisongFlipping.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiClientAuthServiceTest {

    private AiClientAuthService aiClientAuthService;

    @BeforeEach
    void setUp() {
        aiClientAuthService = new AiClientAuthService("website-key", "discord-key");
    }

    @Test
    void isValidAcceptsCorrectKeyForKnownClient() {
        assertTrue(aiClientAuthService.isValid("website", "website-key"));
        assertTrue(aiClientAuthService.isValid("discord", "discord-key"));
    }

    @Test
    void isValidRejectsWrongKey() {
        assertFalse(aiClientAuthService.isValid("website", "wrong-key"));
    }

    @Test
    void isValidRejectsUnknownClientId() {
        assertFalse(aiClientAuthService.isValid("bogus-client", "website-key"));
    }

    @Test
    void isValidRejectsNullClientIdOrKey() {
        assertFalse(aiClientAuthService.isValid(null, "website-key"));
        assertFalse(aiClientAuthService.isValid("website", null));
    }

    @Test
    void isValidRejectsCrossClientKey() {
        assertFalse(aiClientAuthService.isValid("website", "discord-key"));
    }
}
