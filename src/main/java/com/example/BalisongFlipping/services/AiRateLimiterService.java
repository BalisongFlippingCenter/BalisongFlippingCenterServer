package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.utils.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AiRateLimiterService {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Value("${ai.rate-limit.max-requests-per-minute}")
    private int maxRequestsPerMinute;

    public boolean tryAcquire(String sessionId) {
        return rateLimiter.tryAcquire(sessionId, maxRequestsPerMinute, Duration.ofMinutes(1));
    }
}
