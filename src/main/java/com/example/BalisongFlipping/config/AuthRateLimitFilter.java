package com.example.BalisongFlipping.config;

import com.example.BalisongFlipping.utils.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private enum Bucket {
        LOGIN(20, Duration.ofMinutes(15)),
        REGISTER(5, Duration.ofHours(1)),
        CODE_SEND(10, Duration.ofHours(1)),
        CODE_CHECK(20, Duration.ofMinutes(15));

        final int max;
        final Duration window;

        Bucket(int max, Duration window) {
            this.max = max;
            this.window = window;
        }
    }

    private record Rule(HttpMethod method, String pattern, Bucket bucket) {}

    private static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/auth/login", Bucket.LOGIN),
            new Rule(HttpMethod.POST, "/auth/verify-admin-login", Bucket.LOGIN),
            new Rule(HttpMethod.POST, "/auth/register", Bucket.REGISTER),
            new Rule(HttpMethod.POST, "/auth/resend-email-token/*", Bucket.CODE_SEND),
            new Rule(HttpMethod.POST, "/auth/forgot-password", Bucket.CODE_SEND),
            new Rule(HttpMethod.POST, "/accounts/me/request-email-change", Bucket.CODE_SEND),
            new Rule(HttpMethod.POST, "/accounts/me/request-password-change", Bucket.CODE_SEND),
            new Rule(HttpMethod.GET, "/auth/verify-email-token/*", Bucket.CODE_CHECK),
            new Rule(HttpMethod.POST, "/auth/confirm-forgot-password", Bucket.CODE_CHECK),
            new Rule(HttpMethod.POST, "/accounts/me/confirm-email-change", Bucket.CODE_CHECK),
            new Rule(HttpMethod.POST, "/accounts/me/confirm-password-change", Bucket.CODE_CHECK)
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RateLimiter rateLimiter = new RateLimiter();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        for (Rule rule : RULES) {
            if (rule.method().matches(request.getMethod()) && pathMatcher.match(rule.pattern(), path)) {
                Bucket bucket = rule.bucket();
                if (!rateLimiter.tryAcquire(bucket.name() + ":" + clientIp(request), bucket.max, bucket.window)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                    response.getWriter().write("Too many requests. Please try again later.");
                    return;
                }
                break;
            }
        }

        chain.doFilter(request, response);
    }

    // CloudFront appends the viewer's address as the last X-Forwarded-For entry, and the
    // backend only accepts traffic from CloudFront, so earlier (client-supplied) entries are ignored
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return request.getRemoteAddr();

        String[] hops = forwarded.split(",");
        return hops[hops.length - 1].trim();
    }
}
