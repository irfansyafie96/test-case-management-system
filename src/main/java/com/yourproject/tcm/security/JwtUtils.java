package com.yourproject.tcm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JwtUtils - JWT (JSON Web Token) Utility Class
 *
 * Handles JWT token operations:
 * 1. Generate tokens during login
 * 2. Validate tokens on incoming requests
 * 3. Extract user information from tokens
 *
 * JWT Structure: Header.Payload.Signature
 * - Header: Contains token type and algorithm
 * - Payload: Contains user information (username, expiration, etc.)
 * - Signature: Ensures token integrity
 */
@Component  // Spring component that can be injected into other classes
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);  // For logging

    /**
     * JWT secret key from application properties, with fallback default
     * This key is used to sign and verify JWT tokens
     * In production, this should be a strong, secret key stored securely
     */
    @Value("${tcm.app.jwtSecret}")
    private String jwtSecret;

    /**
     * JWT token expiration time in milliseconds (default: 24 hours = 86400000 ms)
     * After this time, the token becomes invalid and user must log in again
     */
    @Value("${tcm.app.jwtExpirationMs:86400000}") // 24 hours
    private int jwtExpirationMs;

    /**
     * Get the signing key for JWT operations
     * Converts the JWT secret string to a proper cryptographic key
     * @return SecretKey for signing/verifying JWT tokens
     */
    private SecretKey getSigningKey() {
        // Decode the base64-encoded secret key
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        // Create HMAC SHA key for signing JWT tokens
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for authenticated user
     * Used during login when credentials are valid
     * @param authentication Spring Security Authentication object containing user details
     * @return JWT token string
     */
    public String generateJwtToken(Authentication authentication) {
        // Extract user details from authentication principal
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Create JWT token with user information
        return Jwts.builder()
                .subject(userPrincipal.getUsername())  // Set username as subject
                .issuedAt(new Date())  // Set current time as issued time
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))  // Set expiration time
                .signWith(getSigningKey())  // Sign token with secret key
                .compact();  // Build and compact into token string
    }

    /**
     * Generate a JWT token directly from username
     * Alternative method for token generation (not used in login flow)
     * @param username Username to include in token
     * @return JWT token string
     */
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)  // Set username as subject
                .issuedAt(new Date())  // Set current time as issued time
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))  // Set expiration time
                .signWith(getSigningKey())  // Sign token with secret key
                .compact();  // Build and compact into token string
    }

    /**
     * Extract username from JWT token
     * Used to identify user from token on authenticated requests
     * @param token JWT token string
     * @return Username extracted from token
     */
    public String getUserNameFromJwtToken(String token) {
        // Parse and verify token, then extract the subject (username)
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Validate JWT token
     * Checks if token is properly formatted, not expired, and has valid signature
     * @param authToken JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            // Parse and verify the token - will throw exception if invalid
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;  // Token is valid
        } catch (MalformedJwtException e) {
            // Token is malformed (invalid format)
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // Token has expired
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // Token uses unsupported algorithm
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // Token claims string is empty
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            // Any other token validation error
            logger.error("JWT token validation failed: {}", e.getMessage());
        }

        return false;  // Token is invalid
    }
}
