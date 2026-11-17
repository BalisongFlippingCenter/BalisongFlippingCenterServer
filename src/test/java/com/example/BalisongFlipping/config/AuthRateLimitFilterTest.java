package com.example.BalisongFlipping.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    private MockHttpServletResponse send(String method, String path, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setContextPath("/api");
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() != 429) assertNotNull(chain.getRequest());
        return response;
    }

    @Test
    void blocksTheSixthRegistrationFromOneIpWithinAnHour() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("POST", "/auth/register", "1.1.1.1").getStatus());
        }

        MockHttpServletResponse blocked = send("POST", "/auth/register", "1.1.1.1");
        assertEquals(429, blocked.getStatus());
        assertEquals("Too many requests. Please try again later.", blocked.getContentAsString());
    }

    @Test
    void tracksEachIpSeparately() throws Exception {
        for (int i = 0; i < 5; i++) send("POST", "/auth/register", "1.1.1.1");

        assertEquals(200, send("POST", "/auth/register", "2.2.2.2").getStatus());
    }

    @Test
    void sharesOneBucketAcrossLoginEndpoints() throws Exception {
        for (int i = 0; i < 10; i++) send("POST", "/auth/login", "1.1.1.1");
        for (int i = 0; i < 10; i++) send("POST", "/auth/verify-admin-login", "1.1.1.1");

        assertEquals(429, send("POST", "/auth/login", "1.1.1.1").getStatus());
    }

    @Test
    void matchesPathVariablesInCodeEndpoints() throws Exception {
        for (int i = 0; i < 20; i++) send("GET", "/auth/verify-email-token/" + i, "1.1.1.1");

        assertEquals(429, send("GET", "/auth/verify-email-token/999999", "1.1.1.1").getStatus());
    }

    @Test
    void usesTheLastForwardedForEntrySoClientsCannotSpoofIt() throws Exception {
        for (int i = 0; i < 5; i++) send("POST", "/auth/register", "9.9.9." + i + ", 1.1.1.1");

        assertEquals(429, send("POST", "/auth/register", "8.8.8.8, 1.1.1.1").getStatus());
    }

    @Test
    void fallsBackToTheRemoteAddressWithoutForwardedFor() throws Exception {
        for (int i = 0; i < 5; i++) send("POST", "/auth/register", null);

        assertEquals(429, send("POST", "/auth/register", null).getStatus());
        assertEquals(200, send("POST", "/auth/register", "1.1.1.1").getStatus());
    }

    @Test
    void ignoresOtherEndpointsAndMethods() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertEquals(200, send("GET", "/posts/any", "1.1.1.1").getStatus());
            assertEquals(200, send("GET", "/auth/register", "1.1.1.1").getStatus());
        }
    }

    @Test
    void clientIpPrefersLastForwardedEntry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.7 ");

        assertEquals("203.0.113.7", AuthRateLimitFilter.clientIp(request));
    }

    @Test
    void blockedRequestsDoNotReachTheController() throws Exception {
        for (int i = 0; i < 5; i++) send("POST", "/auth/register", "1.1.1.1");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setContextPath("/api");
        request.addHeader("X-Forwarded-For", "1.1.1.1");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(chain.getRequest());
    }
}
