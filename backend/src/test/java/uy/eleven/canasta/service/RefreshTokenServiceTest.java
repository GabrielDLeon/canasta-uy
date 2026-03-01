package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.config.JwtProperties;
import uy.eleven.canasta.exception.InvalidTokenException;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.model.RefreshToken;
import uy.eleven.canasta.repository.RefreshTokenRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProperties jwtProperties;
    @InjectMocks private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshTokenCreatesActiveTokenWithTtl() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        when(jwtProperties.getRefreshTokenTtl()).thenReturn(3_600_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, RefreshToken.class));

        RefreshToken token = refreshTokenService.createRefreshToken(client);

        assertNotNull(token.getTokenValue());
        assertFalse(token.isRevoked());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void validateRefreshTokenThrowsWhenMissing() {
        when(refreshTokenRepository.findByTokenValue("missing")).thenReturn(Optional.empty());
        assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.validateRefreshToken("missing"));
    }

    @Test
    void validateRefreshTokenThrowsWhenRevoked() {
        RefreshToken token =
                TestDataFactory.refreshToken(
                        1L,
                        TestDataFactory.client(1L, "user@test.com"),
                        "tok",
                        LocalDateTime.now().plusDays(1),
                        true);
        when(refreshTokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token));

        assertThrows(
                InvalidTokenException.class, () -> refreshTokenService.validateRefreshToken("tok"));
    }

    @Test
    void validateRefreshTokenThrowsWhenExpired() {
        RefreshToken token =
                TestDataFactory.refreshToken(
                        1L,
                        TestDataFactory.client(1L, "user@test.com"),
                        "tok",
                        LocalDateTime.now().minusMinutes(1),
                        false);
        when(refreshTokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token));

        assertThrows(
                InvalidTokenException.class, () -> refreshTokenService.validateRefreshToken("tok"));
    }

    @Test
    void rotateRefreshTokenRevokesOldAndReturnsNew() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        RefreshToken oldToken =
                TestDataFactory.refreshToken(
                        1L, client, "old", LocalDateTime.now().plusDays(1), false);
        RefreshToken newToken =
                TestDataFactory.refreshToken(
                        2L, client, "new", LocalDateTime.now().plusDays(1), false);

        when(refreshTokenRepository.findByTokenValue("old")).thenReturn(Optional.of(oldToken));
        when(jwtProperties.getRefreshTokenTtl()).thenReturn(3_600_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(oldToken, newToken);

        RefreshTokenService.TokenPair pair = refreshTokenService.rotateRefreshToken("old");

        assertEquals("new", pair.refreshToken());
        assertTrue(oldToken.isRevoked());
        assertNotNull(oldToken.getRevokedAt());
    }

    @Test
    void revokeAllUserRefreshTokensRevokesEveryToken() {
        RefreshToken a =
                TestDataFactory.refreshToken(
                        1L,
                        TestDataFactory.client(1L, "user@test.com"),
                        "a",
                        LocalDateTime.now().plusDays(1),
                        false);
        RefreshToken b =
                TestDataFactory.refreshToken(
                        2L,
                        TestDataFactory.client(1L, "user@test.com"),
                        "b",
                        LocalDateTime.now().plusDays(1),
                        false);
        when(refreshTokenRepository.findByClientClientIdAndIsRevokedFalse(1L))
                .thenReturn(List.of(a, b));

        refreshTokenService.revokeAllUserRefreshTokens(1L);

        assertTrue(a.isRevoked());
        assertTrue(b.isRevoked());
        verify(refreshTokenRepository).saveAll(List.of(a, b));
    }

    @Test
    void cleanupExpiredTokensDeletesExpiredList() {
        List<RefreshToken> expired =
                List.of(
                        TestDataFactory.refreshToken(
                                1L,
                                TestDataFactory.client(1L, "user@test.com"),
                                "expired",
                                LocalDateTime.now().minusDays(1),
                                false));
        when(refreshTokenRepository.findByIsRevokedFalseAndExpiresAtBefore(
                        any(LocalDateTime.class)))
                .thenReturn(expired);

        refreshTokenService.cleanupExpiredTokens();

        ArgumentCaptor<List<RefreshToken>> captor = ArgumentCaptor.forClass(List.class);
        verify(refreshTokenRepository).deleteAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }
}
