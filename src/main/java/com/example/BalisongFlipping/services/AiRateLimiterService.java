package com.example.BalisongFlipping.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiRateLimiterService {

    private static final long WINDOW_MILLIS = 60_000;

    private record Window(long windowStartMillis, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${ai.rate-limit.max-requests-per-minute}")
    private int maxRequestsPerMinute;

    public boolean tryAcquire(String sessionId) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(sessionId, (id, existing) -> {
            if (existing == null || now - existing.windowStartMillis() >= WINDOW_MILLIS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= maxRequestsPerMinute;
    }
}
