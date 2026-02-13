package uy.eleven.canasta.security.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uy.eleven.canasta.config.JwtProperties;

/**
 * Utility class for JWT token operations.
 * 
 * This is "plumbing code" - the JWT standard is well-established.
 * You don't need to memorize this, just understand WHAT it does:
 * 1. Generate tokens with claims (data payload)
 * 2. Validate tokens (signature + expiration)
 * 3. Extract information from tokens
 * 
 * Why jjwt library?
 * - Industry standard for Java
 * - Handles crypto correctly (prevents common vulnerabilities)
 * - Clean API, actively maintained
 * 
 * Security considerations:
 * - HS256 (HMAC SHA-256): Fast, symmetric key (same key to sign/verify)
 * - Alternative: RS256 (RSA): Slower, asymmetric (private to sign, public to verify)
 * - For microservices: RS256 is better (services can verify without sharing secret)
 * - For monolith: HS256 is simpler and faster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * Generates an access token for a client.
     * 
     * @param clientId   the client's database ID
     * @param username   the client's username
     * @return signed JWT string
     * 
     * Claims included:
     * - sub (subject): clientId (standard JWT claim)
     * - username: for display purposes
     * - iat: issued at timestamp
     * - exp: expiration timestamp
     */
    public String generateAccessToken(Long clientId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        
        return Jwts.builder()
                .claims(claims)
                .subject(clientId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenTtl()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts clientId from a token.
     * 
     * @param token the JWT string
     * @return clientId as Long
     */
    public Long extractClientId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }

    /**
     * Extracts username from a token.
     * 
     * @param token the JWT string
     * @return username
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /**
     * Extracts expiration date from a token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor - reusable pattern.
     * 
     * @param token          the JWT
     * @param claimsResolver function to extract specific claim
     * @return the extracted value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates a token:
     * 1. Signature is correct (was signed with our secret)
     * 2. Token hasn't expired
     * 3. Token is properly formed
     * 
     * @param token the JWT to validate
     * @return true if valid, false otherwise
     * 
     * Note: This is STATELESS - no DB lookup required.
     * That's why JWT is fast for authentication.
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid token format: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if token is expired.
     * 
     * @param token the JWT
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parses the token and returns all claims.
     * 
     * @param token the JWT
     * @return Claims object containing all payload data
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates the signing key from the configured secret.
     * 
     * @return SecretKey for HS256 algorithm
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
