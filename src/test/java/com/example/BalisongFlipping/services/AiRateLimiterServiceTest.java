package com.example.BalisongFlipping.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRateLimiterServiceTest {

    private AiRateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AiRateLimiterService();
        ReflectionTestUtils.setField(rateLimiter, "maxRequestsPerMinute", 3);
    }

    @Test
    void tryAcquireAllowsUpToConfiguredLimit() {
        assertTrue(rateLimiter.tryAcquire("session-1"));
        assertTrue(rateLimiter.tryAcquire("session-1"));
        assertTrue(rateLimiter.tryAcquire("session-1"));
    }

    @Test
    void tryAcquireRejectsRequestBeyondLimit() {
        rateLimiter.tryAcquire("session-1");
        rateLimiter.tryAcquire("session-1");
        rateLimiter.tryAcquire("session-1");

        assertFalse(rateLimiter.tryAcquire("session-1"));
    }

    @Test
    void tryAcquireTracksIndependentWindowsPerSession() {
        rateLimiter.tryAcquire("session-1");
        rateLimiter.tryAcquire("session-1");
        rateLimiter.tryAcquire("session-1");

        assertTrue(rateLimiter.tryAcquire("session-2"));
    }
}
