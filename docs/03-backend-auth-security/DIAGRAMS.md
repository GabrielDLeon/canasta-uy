# Phase 3: Authentication & Security - Sequence Diagrams

This document contains sequence diagrams illustrating the authentication flows in the CanastaUY API.

---

## Diagram 1: User Registration and First API Key Creation

```
┌──────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │AuthController│  │ClientService │  │ApiKeyService │  │PostgreSQL    │
└──┬───┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │             │                 │                 │                 │
   │  POST /register               │                 │                 │
   │  {user,pass,email}            │                 │                 │
   │──────────────▶│               │                 │                 │
   │               │  register(user, pass, email)    │                 │
   │               │────────────────▶│               │                 │
   │               │                 │                 │                 │
   │               │                 │  save(client)   │                 │
   │               │                 │────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │  return client  │                 │
   │               │                 │◀────────────────│                 │
   │               │                 │                 │                 │
   │  201 Created  │                 │                 │                 │
   │  {clientId, username}          │                 │                 │
   │◀──────────────│                 │                 │                 │
   │               │                 │                 │                 │

   [Luego el usuario crea su primera API Key...]

   │  POST /account/api-keys       │                 │                 │
   │  Authorization: Bearer JWT    │                 │                 │
   │──────────────▶│               │                 │                 │
   │               │  createApiKey(clientId, "Mi Key")                 │
   │               │──────────────────────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │                 │  generate()     │
   │               │                 │                 │  "sk_live_abc.."│
   │               │                 │                 │◀──┐             │
   │               │                 │                 │   │             │
   │               │                 │                 │  INSERT         │
   │               │                 │                 │  api_keys       │
   │               │                 │                 │───────────────▶│
   │               │                 │                 │                │
   │               │                 │                 │  return ApiKey │
   │               │                 │                 │◀───────────────│
   │               │                 │                 │                │
   │  201 Created  │                 │                 │                │
   │  {id, name, keyValue: "sk_live_abc..."}           │                │
   │  [SOLO SE MUESTRA UNA VEZ]      │                 │                │
   │◀──────────────│                 │                 │                │
   │               │                 │                 │                │
```

**Key Learning Points:**
- Registration creates the client account only
- API keys are created separately via the account management endpoint
- The full API key is returned **only once** at creation time
- Subsequent list operations show partial keys (e.g., `sk_live_abc...xyz`)

---

## Diagram 2: Data Access with API Key (Cache HIT)

```
┌──────┐  ┌────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │RateLimitFilter │  │ApiKeyAuthFilter│  │ApiKeyService │  │Redis/Postgre │
└──┬───┘  └────────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │               │                 │                 │                 │
   │  GET /products                 │                 │                 │
   │  Authorization: Bearer sk_live_abc...            │                 │
   │──────────────▶│               │                 │                 │
   │               │               │                 │                 │
   │               │  checkRateLimit("sk_live_abc..")│                 │
   │               │────────────────▶│               │                 │
   │               │                 │  INCR rate_limit:sk_live..:hour │
   │               │                 │  TTL 1h         │                 │
   │               │                 │────────────────▶│                 │
   │               │                 │  return count=5 │                 │
   │               │                 │◀────────────────│                 │
   │               │  5 < 100 ✓      │                 │                 │
   │               │◀────────────────│                 │                 │
   │               │                 │                 │                 │
   │               │  validateApiKey("sk_live_abc..")│                 │
   │               │──────────────────────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │                 │  GET api_key:sk_live.. │
   │               │                 │                 │  from Redis     │
   │               │                 │                 │───────────────▶│
   │               │                 │                 │                │
   │               │                 │                 │  Cache HIT!    │
   │               │                 │                 │  return Client │
   │               │                 │                 │◀───────────────│
   │               │                 │                 │                │
   │               │                 │                 │  SET last_used │
   │               │                 │                 │  (solo Redis)  │
   │               │                 │◀────────────────│                │
   │               │◀────────────────│                 │                │
   │  200 OK       │                 │                 │                │
   │  [{products}] │                 │                 │                │
   │◀──────────────│                 │                 │                │
   │               │                 │                 │                │
```

**Key Learning Points:**
- **Rate limiting happens first**: Checks if user exceeded 100 req/hour
- **Redis cache is checked before PostgreSQL**: ~0.1ms vs ~5-10ms
- **Last used timestamp is updated in Redis only**: No DB write on cache hit
- **Cache hit rate expected: >95%**: Dramatically reduces database load

---

## Diagram 3: Data Access with API Key (Cache MISS)

```
┌──────┐  ┌────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │RateLimitFilter │  │ApiKeyAuthFilter│  │ApiKeyService │  │Redis         │  │PostgreSQL    │
└──┬───┘  └────────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │               │                 │                 │                 │                 │
   │  GET /products                 │                 │                 │                 │
   │  Authorization: Bearer sk_live_xyz...                              │                 │
   │──────────────▶│               │                 │                 │                 │
   │               │               │                 │                 │                 │
   │               │  [Rate limit check... OK]       │                 │                 │
   │               │──────────────────────────────────▶│                 │                 │
   │               │                 │                 │                 │                 │
   │               │                 │                 │  GET api_key:sk_live_xyz..      │
   │               │                 │                 │────────────────▶│                 │
   │               │                 │                 │                 │                 │
   │               │                 │                 │  Cache MISS (null)              │
   │               │                 │                 │◀────────────────│                 │
   │               │                 │                 │                 │                 │
   │               │                 │                 │  SELECT * FROM api_keys         │
   │               │                 │                 │  WHERE key_value = 'sk_live..'  │
   │               │                 │                 │──────────────────────────────────▶│
   │               │                 │                 │                 │                │
   │               │                 │                 │  return ApiKey  │                │
   │               │                 │                 │◀──────────────────────────────────│
   │               │                 │                 │                 │                │
   │               │                 │                 │  SET api_key:sk_live_xyz..      │
   │               │                 │                 │  Client (TTL 1h)│                │
   │               │                 │                 │────────────────▶│                │
   │               │                 │◀────────────────│                 │                │
   │               │◀────────────────│                 │                 │                │
   │  200 OK       │                 │                 │                 │                │
   │◀──────────────│                 │                 │                 │                │
   │               │                 │                 │                 │                │
```

**Key Learning Points:**
- **Cache miss adds latency**: Must query PostgreSQL (~5-10ms)
- **Cache-aside pattern**: Application manages the cache, not the database
- **Result is cached for 1 hour**: Subsequent requests will be cache hits
- **Only ~5% of requests should be cache misses**: After warm-up period

---

## Diagram 4: API Key Revocation (Immediate Invalidation)

```
┌──────┐  ┌──────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │AccountController │  │ApiKeyService │  │PostgreSQL    │  │Redis         │
└──┬───┘  └────────┬─────────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │               │                 │                 │                 │
   │  DELETE /account/api-keys/42    │                 │                 │
   │  Authorization: Bearer JWT      │                 │                 │
   │──────────────▶│               │                 │                 │
   │               │               │                 │                 │
   │               │  revokeApiKey(clientId, 42)     │                 │
   │               │────────────────▶│               │                 │
   │               │                 │                 │                 │
   │               │                 │  UPDATE api_keys                │
   │               │                 │  SET is_active = false,         │
   │               │                 │      revoked_at = now()         │
   │               │                 │  WHERE id = 42                  │
   │               │                 │────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │  return ApiKey  │                 │
   │               │                 │◀────────────────│                 │
   │               │                 │                 │                 │
   │               │                 │  DEL api_key:sk_live_abc..      │
   │               │                 │  (immediate invalidation)       │
   │               │                 │──────────────────────────────────▶│
   │               │                 │                 │                 │
   │  200 OK       │                 │                 │                 │
   │  {message: "Key revoked"}       │                 │                 │
   │◀──────────────│                 │                 │                 │
   │               │                 │                 │                 │

   [Any subsequent request with this key...]

   │  GET /products                 │                 │                 │
   │  Authorization: Bearer sk_live_abc... (revoked)  │                 │
   │──────────────▶│               │                 │                 │
   │               │               │                 │                 │
   │               │  [Rate limit check...]          │                 │
   │               │──────────────────────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │  GET api_key:sk_live_abc..      │
   │               │                 │────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │  null (deleted) │                 │
   │               │                 │◀────────────────│                 │
   │               │                 │                 │                 │
   │               │                 │  SELECT FROM api_keys           │
   │               │                 │  WHERE key_value = 'sk_live..'  │
   │               │                 │  AND is_active = true           │
   │               │                 │──────────────────────────────────▶│
   │               │                 │                 │                 │
   │               │                 │  No results (is_active = false) │
   │               │                 │◀──────────────────────────────────│
   │               │                 │                 │                 │
   │               │  401 Unauthorized               │                 │
   │               │  {error: "Invalid or revoked API key"}            │
   │◀──────────────│                 │                 │                 │
   │               │                 │                 │                 │
```

**Key Learning Points:**
- **Immediate invalidation**: Redis entry is deleted synchronously
- **Soft delete in PostgreSQL**: `is_active = false` preserves audit trail
- **Subsequent requests fail immediately**: No 1-hour cache delay
- **Trade-off**: Slightly slower revocation (synchronous Redis delete) vs. better security

---

## Diagram 5: JWT Authentication Flow (Login → Access Protected Resource)

```
┌──────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │AuthController│  │ClientService │  │JwtService    │  │PostgreSQL    │
└──┬───┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │             │                 │                 │                 │
   │  POST /login                  │                 │                 │
   │  {email, password}            │                 │                 │
   │──────────────▶│               │                 │                 │
   │               │               │                 │                 │
   │               │  login(email, password)         │                 │
   │               │────────────────▶│               │                 │
   │               │                 │                 │                 │
   │               │                 │  SELECT * FROM clients          │
   │               │                 │  WHERE email = ?                │
   │               │                 │────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │  return Client  │                 │
   │               │                 │◀────────────────│                 │
   │               │                 │                 │                 │
   │               │                 │  BCrypt.checkpw(password, hash) │
   │               │                 │  ✓ Password matches             │
   │               │                 │                 │                 │
   │               │  return Client  │                 │                 │
   │               │◀────────────────│                 │                 │
   │               │                 │                 │                 │
   │               │  generateTokens(clientId)       │                 │
   │               │──────────────────────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │                 │  Create JWT:    │
   │               │                 │                 │  {              │
   │               │                 │                 │    sub: clientId│
   │               │                 │                 │    username: u  │
   │               │                 │                 │    iat: now     │
   │               │                 │                 │    exp: +15min  │
   │               │                 │                 │  }              │
   │               │                 │                 │                 │
   │               │                 │                 │  Create refresh:│
   │               │                 │                 │  UUID.random()  │
   │               │                 │                 │  (store in DB)  │
   │               │                 │                 │────────────────▶│
   │               │                 │                 │                 │
   │  200 OK       │                 │                 │                 │
   │  {            │                 │                 │                 │
   │    accessToken: "eyJhbG...",    │                 │                 │
   │    refreshToken: "uuid-abc...", │                 │                 │
   │    expiresIn: 900              │                 │                 │
   │  }            │                 │                 │                 │
   │◀──────────────│                 │                 │                 │
   │               │                 │                 │                 │

   [Later: Access protected endpoint with JWT]

   │  GET /account/profile          │                 │                 │
   │  Authorization: Bearer eyJhbG...                │                 │
   │──────────────▶│               │                 │                 │
   │               │               │                 │                 │
   │               │  [JwtAuthFilter intercepts]     │                 │
   │               │  validateToken(jwt)             │                 │
   │               │──────────────────────────────────▶│                 │
   │               │                 │                 │                 │
   │               │                 │                 │  Parse JWT:     │
   │               │                 │                 │  - Verify sig   │
   │               │                 │                 │  - Check exp    │
   │               │                 │                 │  - Extract sub  │
   │               │                 │                 │                 │
   │               │  ✓ Valid        │                 │                 │
   │               │◀──────────────────────────────────│                 │
   │               │                 │                 │                 │
   │               │  Set SecurityContext            │                 │
   │               │  (clientId, ROLE_USER)          │                 │
   │               │──────────────────────────────┐  │                 │
   │               │                              │  │                 │
   │               │  getProfile(clientId)        │  │                 │
   │               │──────────────────────────────▶│  │                 │
   │               │                              │  │                 │
   │               │  return Profile              │  │                 │
   │               │◀──────────────────────────────│  │                 │
   │               │                 │                 │                 │
   │  200 OK       │                 │                 │                 │
   │  {profile data}                │                 │                 │
   │◀──────────────│                 │                 │                 │
   │               │                 │                 │                 │
```

**Key Learning Points:**
- **JWT is stateless**: No DB lookup needed for validation (signature verification only)
- **Access token short-lived**: 15 minutes, contains claims (clientId, username)
- **Refresh token long-lived**: 7 days, stored in PostgreSQL (can be revoked)
- **Security context**: Spring Security tracks authenticated user for the request

---

## Diagram 6: Refresh Token Rotation

```
┌──────┐  ┌──────────────┐  ┌──────────────────┐  ┌──────────────┐  ┌──────────────┐
│Client│  │AuthController│  │RefreshTokenService│  │JwtService    │  │PostgreSQL    │
└──┬───┘  └──────┬───────┘  └──────┬───────────┘  └──────┬───────┘  └──────┬───────┘
   │             │                 │                     │                 │
   │  POST /refresh                 │                     │                 │
   │  {refreshToken: "uuid-abc..."} │                     │                 │
   │──────────────▶│               │                     │                 │
   │               │               │                     │                 │
   │               │  rotateToken(refreshToken)          │                 │
   │               │────────────────▶│                   │                 │
   │               │                 │                     │                 │
   │               │                 │  SELECT * FROM refresh_tokens     │
   │               │                 │  WHERE token_value = 'uuid-abc..' │
   │               │                 │  AND is_revoked = false           │
   │               │                 │  AND expires_at > now()           │
   │               │                 │───────────────────────────────────▶│
   │               │                 │                     │                │
   │               │                 │  return RefreshToken│                │
   │               │                 │◀────────────────────│                │
   │               │                 │                     │                │
   │               │                 │                     │                │
   │               │                 │  [ROTATION - Single Use Pattern]    │
   │               │                 │                     │                │
   │               │                 │  1. Revoke old token│                │
   │               │                 │  UPDATE refresh_tokens              │
   │               │                 │  SET is_revoked = true              │
   │               │                 │  WHERE token_value = 'uuid-abc..'   │
   │               │                 │───────────────────────────────────▶│
   │               │                 │                     │                │
   │               │                 │  2. Generate new refresh            │
   │               │                 │  UUID.random()      │                │
   │               │                 │  INSERT refresh_tokens            │
   │               │                 │───────────────────────────────────▶│
   │               │                 │                     │                │
   │               │                 │  3. Generate new access             │
   │               │                 │  generateJwt(clientId)              │
   │               │                 │────────────────────▶│                │
   │               │                 │  return new JWT     │                │
   │               │                 │◀────────────────────│                │
   │               │                 │                     │                │
   │  200 OK       │                 │                     │                │
   │  {            │                 │                     │                │
   │    accessToken: "eyJhbG...new", │                     │                │
   │    refreshToken: "uuid-xyz...new"                    │                │
   │  }            │                 │                     │                │
   │◀──────────────│                 │                     │                │
   │               │                 │                     │                │

   [Security benefit: Stolen refresh token can't be reused]

   │  POST /refresh                 │                     │                │
   │  {refreshToken: "uuid-abc..."} │  [SAME OLD TOKEN]   │                │
   │──────────────▶│               │                     │                │
   │               │               │                     │                │
   │               │  rotateToken("uuid-abc...")         │                │
   │               │────────────────▶│                   │                │
   │               │                 │                     │                │
   │               │                 │  SELECT...          │                │
   │               │                 │  is_revoked = true  │                │
   │               │                 │◀────────────────────│                │
   │               │                 │                     │                │
   │               │  401 Unauthorized                   │                │
   │               │  {error: "Token already used"}      │                │
   │◀──────────────│                 │                     │                │
   │               │                 │                     │                │
```

**Key Learning Points:**
- **Single-use pattern**: Refresh token can only be used once
- **Rotation prevents replay attacks**: Stolen token becomes useless after first use
- **Client must store new tokens immediately**: If client crashes after refresh, session is lost
- **Trade-off**: More complex client logic vs. better security

---

## Summary: Authentication Flow Decision Tree

```
                    Incoming Request
                          │
                          ▼
            ┌─────────────────────────┐
            │  What endpoint?         │
            └────────────┬────────────┘
                         │
           ┌─────────────┼─────────────┐
           │             │             │
           ▼             ▼             ▼
    /auth/register  /account/*    /products/*
    /auth/login         │             │
    /auth/refresh       │             │
           │            │             │
           ▼            ▼             ▼
    NO AUTH       JWT Auth      JWT OR API Key
    REQUIRED      REQUIRED        REQUIRED
           │            │             │
           │            │             ├─▶ RateLimitFilter
           │            │             │      (100 req/hour)
           │            │             │
           │            │             ├─▶ ApiKeyAuthFilter
           │            │             │      (validate Api-Key if present)
           │            │             │
           │            │             ├─▶ JwtAuthFilter
           │            │             │      (validate JWT if present)
           │            │             │
           │            │             ▼
           │            │        Controller
           │            │
           │            └─▶ JwtAuthFilter
           │                   (validate JWT)
           │
           └────────────────▶ Controller
```

---

## Performance Comparison

| Operation | With Cache | Without Cache | Improvement |
|-----------|-----------|---------------|-------------|
| API Key Validation | ~0.1ms | ~5-10ms | **50-100x faster** |
| Rate Limit Check | ~0.1ms | ~5-10ms | **50-100x faster** |
| JWT Validation | ~0.05ms | ~0.05ms | Stateless (no cache needed) |
| Expected Hit Rate | >95% | N/A | 19/20 requests cached |

---

**Last Updated**: 2026-03-06
