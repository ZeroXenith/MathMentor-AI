package com.graduation.mathai.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token provider — issues and validates JWT tokens.
 * Secret is configurable via JWT_SECRET environment variable.
 */
@Component
public class JwtTokenProvider {

    /**
     * Token validity: 7 days in milliseconds.
     */
    private static final long EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final SecretKey secretKey;

    public JwtTokenProvider() {
        String secret = System.getenv().getOrDefault("JWT_SECRET", "MathMentor-AI-Default-JWT-Secret-Min-32chars!");
        // Ensure minimum length for HMAC-SHA256 (32 bytes)
        if (secret.length() < 32) {
            secret = secret + secret.repeat((32 / secret.length()) + 1);
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for the given user.
     */
    public String generateToken(long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate and parse token, returning the user ID.
     *
     * @return userId, or null if invalid/expired
     */
    public Long validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
