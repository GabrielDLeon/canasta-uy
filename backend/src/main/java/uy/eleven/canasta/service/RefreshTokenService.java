package uy.eleven.canasta.service;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.eleven.canasta.config.JwtProperties;
import uy.eleven.canasta.exception.InvalidTokenException;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.model.RefreshToken;
import uy.eleven.canasta.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken createRefreshToken(Client client) {
        RefreshToken token = new RefreshToken();
        token.setClient(client);
        token.setTokenValue(UUID.randomUUID().toString());
        token.setExpiresAt(
                LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenTtl() / 1000));
        token.setRevoked(false);

        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenValue(tokenValue)
                        .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        return token;
    }

    @Transactional
    public TokenPair rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = validateRefreshToken(oldTokenValue);

        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(oldToken);

        RefreshToken newToken = createRefreshToken(oldToken.getClient());

        return new TokenPair(newToken.getTokenValue(), oldToken.getClient());
    }

    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenValue(tokenValue)
                        .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        var expiredTokens =
                refreshTokenRepository.findByIsRevokedFalseAndExpiresAtBefore(LocalDateTime.now());

        refreshTokenRepository.deleteAll(expiredTokens);
    }

    public record TokenPair(String refreshToken, Client client) {}
}
