package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.exception.InvalidTokenException;
import uy.eleven.canasta.exception.TokenExpiredException;
import uy.eleven.canasta.security.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock private JwtUtil jwtUtil;
    @InjectMocks private JwtService jwtService;

    @Test
    void generateAccessTokenDelegatesToJwtUtil() {
        when(jwtUtil.generateAccessToken(1L, "user")).thenReturn("jwt-token");

        String token = jwtService.generateAccessToken(1L, "user");

        assertEquals("jwt-token", token);
    }

    @Test
    void validateAndGetClaimsThrowsExpiredException() {
        when(jwtUtil.isTokenValid("expired")).thenReturn(false);
        when(jwtUtil.isTokenExpired("expired")).thenReturn(true);

        assertThrows(TokenExpiredException.class, () -> jwtService.validateAndGetClaims("expired"));
    }

    @Test
    void validateAndGetClaimsThrowsInvalidException() {
        when(jwtUtil.isTokenValid("invalid")).thenReturn(false);
        when(jwtUtil.isTokenExpired("invalid")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> jwtService.validateAndGetClaims("invalid"));
    }

    @Test
    void validateAndGetClaimsReturnsClaimsWhenValid() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.isTokenValid("valid")).thenReturn(true);
        when(jwtUtil.extractClaim(any(), any())).thenReturn(claims);

        Claims result = jwtService.validateAndGetClaims("valid");

        assertEquals(claims, result);
    }

    @Test
    void extractorsValidateThenDelegate() {
        when(jwtUtil.isTokenValid("valid")).thenReturn(true);
        when(jwtUtil.extractClaim(any(), any())).thenReturn(mock(Claims.class));
        when(jwtUtil.extractClientId("valid")).thenReturn(11L);
        when(jwtUtil.extractUsername("valid")).thenReturn("user@test.com");

        assertEquals(11L, jwtService.extractClientId("valid"));
        assertEquals("user@test.com", jwtService.extractUsername("valid"));
        verify(jwtUtil).extractClientId("valid");
        verify(jwtUtil).extractUsername("valid");
    }
}
