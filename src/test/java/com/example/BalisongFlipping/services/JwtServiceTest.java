package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.modals.accounts.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Encoders.BASE64.encode(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    private User user(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        return u;
    }

    @Test
    void generateAccessTokenProducesNonBlankToken() {
        String token = jwtService.generateAccessToken(user("flipper@example.com"));
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractUsernameReturnsSubject() {
        String token = jwtService.generateAccessToken(user("flipper@example.com"));
        assertEquals("flipper@example.com", jwtService.extractUsername(token));
    }

    @Test
    void generateAccessTokenIncludesExtraClaims() {
        String token = jwtService.generateAccessToken(Map.of("role", "ADMIN"), user("flipper@example.com"));
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("ADMIN", role);
    }

    @Test
    void isAccessTokenValidReturnsTrueForMatchingUser() {
        User u = user("flipper@example.com");
        String token = jwtService.generateAccessToken(u);
        assertTrue(jwtService.isAccessTokenValid(token, u));
    }

    @Test
    void isAccessTokenValidReturnsFalseForDifferentUser() {
        String token = jwtService.generateAccessToken(user("flipper@example.com"));
        assertFalse(jwtService.isAccessTokenValid(token, user("someoneelse@example.com")));
    }

    @Test
    void isAccessTokenValidReturnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        User u = user("flipper@example.com");
        String token = jwtService.generateAccessToken(u);

        assertThrows(ExpiredJwtException.class, () -> jwtService.isAccessTokenValid(token, u));
    }

    @Test
    void getExpirationTimeReturnsConfiguredValue() {
        assertEquals(3600000L, jwtService.getExpirationTime());
    }
}
