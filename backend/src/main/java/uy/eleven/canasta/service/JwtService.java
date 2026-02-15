package uy.eleven.canasta.service;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import uy.eleven.canasta.exception.InvalidTokenException;
import uy.eleven.canasta.exception.TokenExpiredException;
import uy.eleven.canasta.security.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;

    public String generateAccessToken(Long clientId, String username) {
        return jwtUtil.generateAccessToken(clientId, username);
    }

    public Claims validateAndGetClaims(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            if (jwtUtil.isTokenExpired(token)) {
                throw new TokenExpiredException("Access token has expired");
            }
            throw new InvalidTokenException("Invalid access token");
        }
        return jwtUtil.extractClaim(token, claims -> claims);
    }

    public Long extractClientId(String token) {
        validateAndGetClaims(token);
        return jwtUtil.extractClientId(token);
    }

    public String extractUsername(String token) {
        validateAndGetClaims(token);
        return jwtUtil.extractUsername(token);
    }

    public boolean isTokenValid(String token) {
        return jwtUtil.isTokenValid(token);
    }
}
