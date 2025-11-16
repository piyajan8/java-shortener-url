package com.macode101.shortenerurl.util;

import com.macode101.shortenerurl.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long";
    private static final Long TEST_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmailShouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals("test@example.com", extractedEmail);
    }

    @Test
    void extractClaimShouldReturnUserId() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        String extractedUserId = jwtUtil.extractClaim(token, claims -> claims.get("userId", String.class));

        assertEquals("123", extractedUserId);
    }

    @Test
    void extractClaimShouldReturnRoles() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER", "ADMIN"));

        List<?> extractedRoles = jwtUtil.extractClaim(token, claims -> claims.get("roles", List.class));

        assertEquals(2, extractedRoles.size());
        assertTrue(extractedRoles.contains("USER"));
        assertTrue(extractedRoles.contains("ADMIN"));
    }

    @Test
    void validateTokenWithValidTokenShouldReturnTrue() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateTokenWithInvalidTokenShouldReturnUnauthorizedException() {
        assertThrows(UnauthorizedException.class,
                () -> jwtUtil.validateToken("invalid.token.here")
        );
    }

    @Test
    void validateTokenWithNullTokenShouldReturnUnauthorizedException() {
        assertThrows(UnauthorizedException.class,
                () -> jwtUtil.validateToken(null)
        );
    }

    @Test
    void isTokenExpiredWithFreshTokenShouldReturnFalse() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void isTokenExpiredWithExpiredTokenShouldReturnUnauthorizedException() throws InterruptedException {
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", 1L);
        
        String token = shortExpirationJwtUtil.generateToken("123", "test@example.com", List.of("USER"));
        Thread.sleep(10);

        assertThrows(UnauthorizedException.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void extractExpirationShouldReturnFutureDate() {
        String token = jwtUtil.generateToken("123", "test@example.com", List.of("USER"));

        Date expiration = jwtUtil.extractExpiration(token);

        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateTokenWithExpiredTokenShouldReturnFalse() throws InterruptedException {
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", 1L);
        
        String token = shortExpirationJwtUtil.generateToken("123", "test@example.com", List.of("USER"));
        Thread.sleep(10);

        assertThrows(UnauthorizedException.class, () -> jwtUtil.validateToken(token));
    }
}
