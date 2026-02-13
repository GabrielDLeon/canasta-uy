package uy.eleven.canasta.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 *
 * <p>Loaded from application.yaml under 'canasta.security.jwt' prefix.
 *
 * <p>Why type-safe properties? - Compile-time validation (no typos in property names) // * - IDE
 * autocomplete support - Default values prevent misconfiguration - Easy to mock in tests
 */
@Configuration
@ConfigurationProperties(prefix = "canasta.security.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Secret key for signing JWTs. MUST be at least 256 bits for HS256 algorithm. In production,
     * use environment variable: JWT_SECRET
     */
    private String secret = "changeme-dev-only-secret-key-must-be-256-bits-long";

    /**
     * Access token TTL in milliseconds. Default: 15 minutes (900,000 ms)
     *
     * <p>Trade-off: - Short TTL (5 min): More secure, but user re-authenticates often - Long TTL (1
     * hour): Better UX, but stolen token is valid longer - 15 min: Industry sweet spot for web apps
     */
    private long accessTokenTtl = 900_000;

    /**
     * Refresh token TTL in milliseconds. Default: 7 days (604,800,000 ms)
     *
     * <p>Why longer than access token? - User shouldn't re-login every 15 minutes - Stored in DB so
     * we can revoke it immediately if needed - Single-use rotation prevents replay attacks
     */
    private long refreshTokenTtl = 604_800_000;
}
