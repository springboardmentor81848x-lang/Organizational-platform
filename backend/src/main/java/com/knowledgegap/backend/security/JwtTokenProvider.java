package com.knowledgegap.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Generates a secure random key for HS256 algorithm
    private final SecretKey jwtSecret = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
    private final long jwtExpirationMs = 86400000; // 24 hours validity

    // Generate JWT token using user email
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    // Extract email from JWT token
    public String getEmailFromJWT(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Validate JWT token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(jwtSecret).build().parse(authToken);
            return true;
        } catch (Exception ex) {
            // Token is invalid, expired, or tampered with
        }
        return false;
    }
}