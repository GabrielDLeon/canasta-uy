---
status: pending
updated: 2026-02-07
---

# Phase 3: Backend - Authentication & Security - PLAN

**Objective**: Implement dual authentication system (JWT + API Keys) with Redis-based caching and rate limiting for the CanastaUY API.

---

## Implementation Strategy

### High-Level Approach

**Phase 3 will add dual authentication to the existing Spring Boot backend:**

### Part A: API Key Authentication (Programmatic Access)

1. **API Key Generation & Storage**
   - Generate secure random keys (256-bit entropy)
   - Store in PostgreSQL `api_keys` table (1-to-many with clients)
   - Format: `sk_live_<64_hex_chars>`
   - Multiple keys per client with optional names

2. **API Key Authentication Filter**
   - Intercept data access requests
   - Extract API key from Authorization header
   - Validate key exists and is active
   - Cache validation result in Redis (1 hour TTL)

3. **Rate Limiting Filter**
   - Count requests per API key per hour
   - Use Redis for fast, scalable tracking
   - Return HTTP 429 if limit exceeded
   - Configurable limit (default: 100/hour)

### Part B: JWT Authentication (Web Dashboard)

4. **JWT Utilities**
   - Generate access tokens (15 min expiration)
   - Generate refresh tokens (7 day expiration)
   - Validate JWT signatures and expiration
   - Parse claims (clientId, username, roles)

5. **JWT Authentication Filter**
   - Intercept account management requests
   - Extract JWT from Authorization header
   - Validate signature and expiration
   - Set Spring Security context

6. **Refresh Token Service**
   - Store refresh tokens in PostgreSQL
   - Implement token rotation (single-use)
   - Handle refresh token revocation
   - Clean up expired tokens

### Part C: Endpoints & Configuration

7. **Auth Endpoints**
   - POST /auth/register - Create account + first API key
   - POST /auth/login - Username/password → JWT tokens
   - POST /auth/refresh - Refresh token → new tokens
   - POST /auth/logout - Revoke refresh token

8. **Account Management Endpoints (JWT)**
   - GET /account/profile - View account info
   - GET /account/api-keys - List API keys
   - POST /account/api-keys - Create new key
   - DELETE /account/api-keys/{id} - Revoke key

9. **Security Configuration**
   - Configure dual authentication (JWT + API Keys)
   - Set up filter chains (order matters)
   - Define authorization rules per endpoint type
   - Exception handlers for auth errors

---

## Architectural Components

### 1. API Key Generator

**Purpose**: Create secure, random API keys

**Design**:
- Use cryptographically secure randomness (not Math.random)
- Generate 256-bit entropy keys
- Format consistently: `sk_live_<64_hex_characters>`
- Guarantee uniqueness across all clients

**Placement**: `security/` package as utility/component
**No database changes**: Uses existing `api_key` column in clients

---

### 2. Client Service (Business Logic)

**Purpose**: Handle client registration and profile management

**Responsibilities**:
- Validate new registrations (unique username/email)
- Hash passwords with BCrypt (never plaintext)
- Create client account
- Retrieve client by ID or username
- Update client profile

**Key decision**: All business logic here, controllers just REST wrappers
**Dependencies**: ClientRepository, PasswordEncoder

---

### 3. API Key Service

**Purpose**: Manage API keys lifecycle

**Responsibilities**:
- Generate new API keys (with optional name)
- Retrieve client's API keys (list)
- Retrieve client by API key (for authentication)
- Revoke specific API key (soft delete)
- Track last_used_at timestamps

**Key decision**: Separate service for API key operations
**Dependencies**: ApiKeyRepository, ApiKeyGenerator, RedisTemplate

---

### 4. JWT Service

**Purpose**: Handle JWT token operations

**Responsibilities**:
- Generate access tokens (15 min TTL)
- Generate refresh tokens (7 day TTL)
- Validate JWT signatures
- Parse JWT claims
- Check token expiration
- Extract clientId from token

**Key decision**: Use jjwt library for JWT operations
**Dependencies**: JwtProperties (secret key, expiration config)

---

### 5. Refresh Token Service

**Purpose**: Manage refresh token lifecycle

**Responsibilities**:
- Store refresh tokens in PostgreSQL
- Validate refresh token (not expired, not revoked)
- Rotate refresh tokens (single-use pattern)
- Revoke refresh token on logout
- Clean up expired tokens (scheduled task)

**Key decision**: Refresh tokens are revocable (stored in DB)
**Dependencies**: RefreshTokenRepository, JwtService

---

### 6. API Key Authentication Filter

**Purpose**: Validate API key for data access requests

**Responsibilities**:
- Extract API key from `Authorization: Bearer <key>` header
- Check Redis cache first (fast path)
- Query PostgreSQL on cache miss
- Validate `is_active = true`
- Update last_used_at timestamp
- Set Spring Security authentication context
- Return 401 Unauthorized if invalid/revoked

**Key optimization**: Redis cache (1h TTL) reduces DB hits 50-100x
**Key decision**: Cache-aside pattern (check cache, fallback to DB)
**Applies to**: Data endpoints (/api/v1/products, /api/v1/prices)

---

### 7. JWT Authentication Filter

**Purpose**: Validate JWT for account management requests

**Responsibilities**:
- Extract JWT from `Authorization: Bearer <token>` header
- Validate JWT signature and expiration
- Parse claims (clientId, username)
- Set Spring Security authentication context
- Return 401 Unauthorized if invalid/expired

**Key optimization**: Stateless validation (no DB lookup)
**Key decision**: Access tokens NOT stored in DB (validated by signature)
**Applies to**: Account endpoints (/api/v1/account/*)

---

### 8. Rate Limiting Filter

**Purpose**: Prevent API abuse

**Responsibilities**:
- Extract API key from request
- Check Redis counter: `rate_limit:<key>:<hour>`
- Increment counter (O(1) operation)
- Set auto-expiring TTL (1 hour)
- Return HTTP 429 if exceeded
- Add rate limit headers to response

**Key optimization**: O(1) Redis operation adds <1% overhead
**Key decision**: Disable in dev profile for easier testing
**Configuration**: 100 requests/hour (conservative limit, adjustable via application.yaml)

---

### 9. Auth Controller

**Purpose**: Expose authentication endpoints (public)

**Endpoints**:

**POST /api/v1/auth/register** (Public)
- Input: username, email, password
- Output: clientId, username, defaultApiKey
- Status: 201 Created
- Creates client + first API key automatically

**POST /api/v1/auth/login** (Public)
- Input: username, password
- Output: accessToken, refreshToken, expiresIn
- Status: 200 OK
- Validates credentials, generates JWT tokens

**POST /api/v1/auth/refresh** (Public)
- Input: refreshToken
- Output: new accessToken, new refreshToken
- Status: 200 OK
- Rotates refresh token (single-use pattern)

**POST /api/v1/auth/logout** (JWT Required)
- Input: JWT in Authorization header
- Effect: Revokes refresh token
- Status: 200 OK

---

### 10. Account Controller

**Purpose**: Expose account management endpoints (JWT required)

**Endpoints**:

**GET /api/v1/account/profile** (JWT Required)
- Output: clientId, username, email, totalKeys, createdAt
- Status: 200 OK

**GET /api/v1/account/api-keys** (JWT Required)
- Output: List of API keys (partial display)
- Status: 200 OK
- Shows: id, name, keyPrefix (sk_live_abc...xyz), isActive, createdAt

**POST /api/v1/account/api-keys** (JWT Required)
- Input: name (optional)
- Output: id, name, keyValue (FULL KEY, shown once)
- Status: 201 Created

**DELETE /api/v1/account/api-keys/{id}** (JWT Required)
- Effect: Revokes specific API key
- Status: 200 OK

---

### 11. Security Configuration

**Purpose**: Configure Spring Security for dual authentication

**What it does**:
- Define authentication rules per endpoint type
- Configure filter chains (JWT filter + API Key filter)
- Set up exception handling (401, 403 errors)
- Enable stateless mode (no sessions, no cookies)
- Disable CSRF protection (not needed for stateless API)

**Authentication rules**:
- `/api/v1/auth/**` (except /logout) → Public
- `/api/v1/account/**` → JWT Required
- `/api/v1/products/**` → API Key Required
- `/api/v1/prices/**` → API Key Required

**Filter order**: RateLimitFilter → JwtAuthFilter → ApiKeyAuthFilter

---

### 7. Redis Configuration

**Purpose**: Enable caching and rate limiting

**What it does**:
- Configure Redis connection (host, port)
- Create RedisTemplate for key-value operations
- Set up serialization (String keys/values)

**Key decision**: Use same Redis instance for both caching and rate limiting

---

### 8. Exception Handling

**Purpose**: Graceful error responses

**Exceptions to handle**:
- InvalidApiKeyException → HTTP 401 Unauthorized
- DuplicateUsernameException → HTTP 409 Conflict
- DuplicateEmailException → HTTP 409 Conflict
- Validation errors → HTTP 400 Bad Request

**Key decision**: Centralized handler (@RestControllerAdvice) for consistency

---

## Data Changes

### Database Schema

**Migration V3 (new)**:

**api_keys table** (new):
- `id` BIGSERIAL PRIMARY KEY
- `client_id` BIGINT (FK to clients)
- `key_value` VARCHAR(74) UNIQUE (sk_live_<64_hex>)
- `name` VARCHAR(100) (optional, e.g., "Production key")
- `is_active` BOOLEAN DEFAULT true
- `last_used_at` TIMESTAMP
- `created_at` TIMESTAMP
- `revoked_at` TIMESTAMP

**refresh_tokens table** (new):
- `id` BIGSERIAL PRIMARY KEY
- `client_id` BIGINT (FK to clients)
- `token_value` VARCHAR(255) UNIQUE
- `expires_at` TIMESTAMP NOT NULL
- `is_revoked` BOOLEAN DEFAULT false
- `created_at` TIMESTAMP
- `revoked_at` TIMESTAMP

**clients table** (modified):
- Removed: `api_key` column (moved to api_keys table)
- Kept: username, email, password, is_active, timestamps

### Configuration Files

**application.yaml** - Add:
- Redis connection details (host, port, timeout)
- Rate limit settings (enabled, max requests per hour)
- JWT settings (secret key, access token TTL, refresh token TTL)

**application-dev.yaml** - Add:
- Rate limit disabled (for testing)
- JWT secret (development only, never commit production secret)

---

## Security Properties

**New configuration properties** under `canasta.security` namespace:

```yaml
canasta.security:
  api-key:
    header-name: "Authorization"
    prefix: "Bearer "
  jwt:
    secret: "${JWT_SECRET:changeme-dev-only}"
    access-token-ttl: 900000      # 15 minutes in ms
    refresh-token-ttl: 604800000  # 7 days in ms
  rate-limit:
    enabled: true
    max-requests-per-hour: 100
```

**Key decision**: Type-safe @ConfigurationProperties classes for easy testing

---

## Package Structure

```
src/main/java/uy/eleven/canasta/
├── security/              [NEW]
│   ├── filters/
│   │   ├── ApiKeyAuthFilter
│   │   ├── JwtAuthFilter
│   │   └── RateLimitFilter
│   ├── util/
│   │   ├── ApiKeyGenerator
│   │   └── JwtUtil
│   └── SecurityConfig (UPDATE)
│
├── service/              [UPDATE]
│   ├── ClientService (NEW)
│   ├── ApiKeyService (NEW)
│   ├── JwtService (NEW)
│   ├── RefreshTokenService (NEW)
│   ├── RateLimitService (NEW)
│   └── ProductService (existing)
│
├── controller/           [UPDATE]
│   ├── AuthController (NEW)
│   ├── AccountController (NEW)
│   └── ProductController (existing)
│
├── dto/                  [NEW]
│   ├── auth/
│   │   ├── RegisterRequest
│   │   ├── RegisterResponse
│   │   ├── LoginRequest
│   │   ├── LoginResponse
│   │   ├── RefreshRequest
│   │   └── RefreshResponse
│   ├── account/
│   │   ├── ProfileResponse
│   │   ├── ApiKeyResponse
│   │   ├── ApiKeyListResponse
│   │   └── CreateApiKeyRequest
│   └── common/
│       └── MessageResponse
│
├── exception/            [NEW]
│   ├── InvalidApiKeyException
│   ├── InvalidCredentialsException
│   ├── InvalidTokenException
│   ├── TokenExpiredException
│   ├── DuplicateUsernameException
│   ├── DuplicateEmailException
│   └── GlobalExceptionHandler
│
├── config/               [UPDATE]
│   ├── RedisConfig (NEW)
│   ├── SecurityProperties (NEW)
│   ├── JwtProperties (NEW)
│   └── SecurityConfig (UPDATE)
│
├── repository/           [UPDATE]
│   ├── ApiKeyRepository (NEW)
│   ├── RefreshTokenRepository (NEW)
│   ├── ClientRepository (existing)
│   └── ProductRepository (existing)
│
└── model/               [UPDATE]
    ├── Client (existing)
    ├── ApiKey (NEW)
    ├── RefreshToken (NEW)
    └── Product (existing)
```

---

## Key Decisions Rationale

### 1. Why Cache API Keys?
- Every request validates the key → DB would be bottleneck
- Redis cache: ~0.1ms vs DB: ~5-10ms per validation
- 1-hour TTL is acceptable (revoked keys visible within 1h)
- Trade-off: Delayed revocation vs faster authentication

### 2. Why Redis for Rate Limiting?
- Must be fast (happens before business logic)
- Must scale (1000s of concurrent users)
- Redis INCR: O(1) operation, sub-millisecond
- PostgreSQL: Too slow for per-request counting
- Alternative (in-memory): Single instance bottleneck, not distributed

### 3. Why Soft Delete (is_active)?
- Preserve audit trail (when was key revoked?)
- Support future analytics (usage patterns)
- Enable "unrevoke" if needed
- Alternative (hard delete): Loses history

### 4. Why SecureRandom, not Random?
- Math.random() is predictable (cryptographically weak)
- SecureRandom: Unpredictable, suitable for security tokens
- 256-bit entropy: 2^256 possible keys (astronomically large)
- Brute-force guessing: Computationally infeasible

### 5. Why BCrypt, not MD5/SHA?
- MD5/SHA: Fast (bad for passwords - enables brute-force)
- BCrypt: Intentionally slow (10ms per hash = ~100 attempts/sec)
- BCrypt strength 12: ~260ms per hash = ~4 attempts/sec
- Salted: Defeats rainbow tables

### 6. Why Open Registration?
- Public data → lower friction = more adoption
- Email verification can be added later if spam becomes issue
- Alternative: Invite-only = bottleneck for users

---

## Testing Strategy

### Manual Testing (Bruno API Client)

1. **Register new client**
   - POST /auth/register with valid credentials
   - Verify: API key returned, 201 Created

2. **Access existing endpoints**
   - GET /products with API key in Authorization header
   - Verify: 200 OK, JSON response
   - Try without header: 401 Unauthorized

3. **View profile**
   - GET /auth/profile with API key
   - Verify: Client data returned

4. **Revoke key**
   - DELETE /auth/revoke
   - Verify: 200 OK, message returned
   - Try same key again: 401 Unauthorized (key revoked)

5. **Rate limiting** (dev profile with rate limit enabled)
   - Make 101 requests with same key
   - Verify: First 100 return 200 OK
   - Verify: 101st returns 429 Too Many Requests

### Automated Testing (Unit + Integration)

- ClientService tests (registration, validation)
- ApiKeyAuthFilter tests (authentication logic)
- RateLimitService tests (rate limit counters)
- AuthController tests (endpoint behavior)
- Integration tests with real Redis/PostgreSQL

---

## Performance Expectations

### Authentication Latency

**Without caching**: 5-10ms (PostgreSQL)
**With caching**: 0.1-1ms (Redis hit) or 5-10ms (cache miss)
**Expected cache hit rate**: >95%
**Effective latency**: <1ms average

### Rate Limiting Overhead

**Redis INCR operation**: <0.1ms
**Negligible impact**: <1% of total request time

### Database Load

**With caching**: ~5% of total authentication checks hit DB
**Expected: 50-100x reduction in auth queries**

---

## Monitoring & Observability

### Key Metrics to Track

1. **Redis cache hit rate** (target: >95%)
2. **Authentication latency** (target: <1ms average)
3. **Rate limit rejections** (should be low, indicates aggressive users)
4. **Failed auth attempts** (track for abuse patterns)
5. **Redis memory usage** (for capacity planning)

### Logging Strategy

- Log successful registrations (IP, timestamp)
- Log failed authentication attempts (key, IP, timestamp)
- Log rate limit violations (key, hour, request count)
- Mask API keys in logs (show only first 10 + last 3 chars)

---

## Known Limitations

### Phase 3 Scope

- No email verification (future enhancement)
- No admin endpoints for managing clients (Phase 4+)
- No API key rotation (Phase 4+)
- No usage analytics dashboard (Phase 4+)
- No IP-based rate limiting (Phase 4+)
- No custom rate limits per client (Phase 4+)

### By Design

- API keys don't auto-expire (unlike JWT)
- Revoked keys visible within 1 hour (cache TTL)
- No way to recover lost API key (user must create new one)

---

## References

- `CONTEXT.md` - Architecture concepts and decisions
- `TASKS.md` - Detailed task checklist
- `../02-backend-domain-persistence/` - Domain layer
- `../01-backend-infrastructure/` - Database schema
- `../../roadmap-backend.md` - Overall backend roadmap

---

**Last Updated**: 2026-02-07
