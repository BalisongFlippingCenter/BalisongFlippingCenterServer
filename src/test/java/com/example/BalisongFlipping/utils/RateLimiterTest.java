package com.example.BalisongFlipping.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void allowsUpToTheLimitThenBlocks() {
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.tryAcquire("key", 3, Duration.ofMinutes(1)));
        }
        assertFalse(rateLimiter.tryAcquire("key", 3, Duration.ofMinutes(1)));
    }

    @Test
    void tracksKeysIndependently() {
        rateLimiter.tryAcquire("a", 1, Duration.ofMinutes(1));

        assertFalse(rateLimiter.tryAcquire("a", 1, Duration.ofMinutes(1)));
        assertTrue(rateLimiter.tryAcquire("b", 1, Duration.ofMinutes(1)));
    }

    @Test
    void resetsOnceTheWindowEnds() throws InterruptedException {
        rateLimiter.tryAcquire("key", 1, Duration.ofMillis(50));
        assertFalse(rateLimiter.tryAcquire("key", 1, Duration.ofMillis(50)));

        Thread.sleep(80);

        assertTrue(rateLimiter.tryAcquire("key", 1, Duration.ofMillis(50)));
    }
}
