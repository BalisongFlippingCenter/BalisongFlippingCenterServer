package com.example.BalisongFlipping.utils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private static final int CALLS_BETWEEN_EVICTIONS = 1000;

    private record Window(long endMillis, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceEviction = new AtomicInteger();

    public boolean tryAcquire(String key, int max, Duration window) {
        long now = System.currentTimeMillis();

        if (callsSinceEviction.incrementAndGet() >= CALLS_BETWEEN_EVICTIONS) {
            callsSinceEviction.set(0);
            windows.values().removeIf(w -> w.endMillis() <= now);
        }

        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.endMillis()) {
                return new Window(now + window.toMillis(), new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return current.count().get() <= max;
    }
}
