---
status: pending
updated: 2026-02-07
---

# Phase 3: Backend - Authentication & Security - TASKS

This phase implements dual authentication (JWT + API Keys) for the CanastaUY API.

---

## Part A: Foundation & Database

### Task 3.1: Run database migration V3

**Description**: Apply migration to create api_keys and refresh_tokens tables.

**Acceptance criteria**:
- [ ] Migration V3__create_auth_tables.sql exists
- [ ] Run migration (mvn flyway:migrate or on Spring Boot startup)
- [ ] Verify tables created: api_keys, refresh_tokens
- [ ] Verify indexes created
- [ ] Verify clients.api_key column dropped

---

### Task 3.2: Create JPA entities (ApiKey, RefreshToken)

**Description**: Create entity classes for api_keys and refresh_tokens tables.

**ApiKey entity**:
- Fields: id, clientId, keyValue, name, isActive, lastUsedAt, createdAt, revokedAt
- Relationship: @ManyToOne with Client
- Indexes: keyValue, clientId, isActive

**RefreshToken entity**:
- Fields: id, clientId, tokenValue, expiresAt, isRevoked, createdAt, revokedAt
- Relationship: @ManyToOne with Client
- Indexes: tokenValue, clientId, expiresAt

**Acceptance criteria**:
- [ ] ApiKey.java created in model/ package
- [ ] RefreshToken.java created in model/ package
- [ ] Client.java updated with @OneToMany relationships
- [ ] JPA annotations correct (@Entity, @Table, @Column, etc.)
- [ ] Repositories created (ApiKeyRepository, RefreshTokenRepository)

---

### Task 3.3: Create ApiKeyGenerator utility

**Description**: Utility to generate cryptographically secure API keys in format `sk_live_<64_hex_characters>`.

**Acceptance criteria**:
- [ ] Component created in security/util/ package
- [ ] Generates 256-bit entropy keys using SecureRandom
- [ ] Keys are unique and unpredictable
- [ ] Format validated: starts with `sk_live_`, total 74 characters
- [ ] No external dependencies beyond Java standard library
- [ ] Can be tested in isolation

---

### Task 3.4: Create JwtUtil utility

**Description**: Utility to generate and validate JWT tokens.

**Responsibilities**:
- Generate access tokens (15 min expiration)
- Generate refresh tokens (random UUID, not JWT)
- Validate JWT signature
- Parse JWT claims (clientId, username, roles)
- Check token expiration

**Acceptance criteria**:
- [ ] Component created in security/util/ package
- [ ] Uses jjwt library (io.jsonwebtoken)
- [ ] Injects JwtProperties for configuration
- [ ] Generates tokens with HS256 algorithm
- [ ] Validates signature and expiration
- [ ] Extracts clientId from token
- [ ] Handles expired/malformed tokens gracefully

---

## Part B: Services (Business Logic)

### Task 3.5: Create ClientService

**Description**: Business logic for client registration and profile management.

**Responsibilities**:
- Register new clients (validate uniqueness, hash password)
- Retrieve client by ID or username
- Validate credentials (for login)
- Update client profile

**Acceptance criteria**:
- [ ] Service created in service/ package
- [ ] Uses ClientRepository for data access
- [ ] Uses PasswordEncoder for password hashing (BCrypt)
- [ ] Validates: unique username, unique email
- [ ] Input validation: username (3-50 chars, alphanumeric+underscore), email (valid format), password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit)
- [ ] Does NOT create API key automatically on registration
- [ ] Throws appropriate exceptions for errors
- [ ] Transactional operations marked correctly (@Transactional)
- [ ] Can be tested with mocks

---

### Task 3.6: Create ApiKeyService

**Description**: Service to manage API keys lifecycle.

**Responsibilities**:
- Generate new API key (with optional name)
- Retrieve client's API keys (list)
- Retrieve client by API key (for authentication)
- Revoke specific API key (soft delete)
- Update last_used_at timestamp

**Acceptance criteria**:
- [ ] Service created in service/ package
- [ ] Injected with ApiKeyRepository, ApiKeyGenerator
- [ ] Create API key method (returns full key once)
- [ ] List API keys method (returns partial keys: sk_live_abc...xyz)
- [ ] Revoke method sets is_active=false + revoked_at timestamp
- [ ] Update last used timestamp method
- [ ] Can be tested with mocks

---

### Task 3.7: Create JwtService

**Description**: Service to handle JWT token operations.

**Responsibilities**:
- Generate access token (JWT, 15 min)
- Validate access token (signature + expiration)
- Extract clientId from token
- Extract username from token

**Acceptance criteria**:
- [ ] Service created in service/ package
- [ ] Uses JwtUtil for token operations
- [ ] Generates tokens with claims: sub (clientId), username, iat, exp
- [ ] Validates tokens and throws exceptions for invalid/expired
- [ ] Returns parsed claims (clientId, username)
- [ ] Can be tested with mocks

---

### Task 3.8: Create RefreshTokenService

**Description**: Service to manage refresh token lifecycle.

**Responsibilities**:
- Generate and store refresh token (7 day expiration)
- Validate refresh token (exists, not expired, not revoked)
- Rotate refresh token (single-use: revoke old, create new)
- Revoke refresh token (on logout)
- Clean up expired tokens (scheduled task)

**Acceptance criteria**:
- [ ] Service created in service/ package
- [ ] Uses RefreshTokenRepository for storage
- [ ] Create method: generates UUID, stores in DB with expiration
- [ ] Validate method: checks expiration and revocation status
- [ ] Rotate method: revokes old token, creates new token
- [ ] Revoke method: sets is_revoked=true + revoked_at timestamp
- [ ] Scheduled cleanup task (runs daily, removes expired tokens)
- [ ] Can be tested with mocks

---

### Task 3.9: Create RateLimitService

**Description**: Service to track and enforce request rate limits using Redis.

**Responsibilities**:
- Check if API key is allowed (under limit)
- Increment request counter in Redis
- Get remaining requests for API key
- Set auto-expiring TTL (1 hour)
- Handle Redis connection gracefully

**Acceptance criteria**:
- [ ] Service created in service/ package
- [ ] Injected with RedisTemplate
- [ ] Uses configuration for max requests per hour
- [ ] Returns boolean: allowed or not
- [ ] Returns remaining request count
- [ ] Handles Redis key generation: `rate_limit:<key>:<hour>`
- [ ] TTL expires automatically (1 hour)
- [ ] Can be tested with embedded Redis

---

## Part C: Filters (Spring Security)

### Task 3.10: Create ApiKeyAuthFilter

**Description**: Spring Security filter to authenticate requests using API keys.

**Responsibilities**:
- Extract API key from Authorization header
- Validate key exists and is active
- Cache validation result in Redis (1h TTL)
- Set Spring Security authentication context
- Return 401 Unauthorized if invalid
- Skip filter for non-API-key endpoints

**Acceptance criteria**:
- [ ] Filter created in security/filters/ package
- [ ] Extends OncePerRequestFilter (Spring Security)
- [ ] Marked as @Component
- [ ] Injection: ApiKeyService, RedisTemplate
- [ ] Handles missing API key gracefully
- [ ] Caches client data in Redis (key: `api_key:<key>`, TTL: 1h)
- [ ] Validates is_active flag
- [ ] Sets authentication context with ROLE_USER
- [ ] Returns 401 for invalid/revoked keys
- [ ] Only applies to /api/v1/products/**, /api/v1/prices/**

---

### Task 3.11: Create JwtAuthFilter

**Description**: Spring Security filter to authenticate requests using JWT.

**Responsibilities**:
- Extract JWT from Authorization header
- Validate JWT signature and expiration
- Parse claims (clientId, username)
- Set Spring Security authentication context
- Return 401 Unauthorized if invalid/expired
- Skip filter for non-JWT endpoints

**Acceptance criteria**:
- [ ] Filter created in security/filters/ package
- [ ] Extends OncePerRequestFilter (Spring Security)
- [ ] Marked as @Component
- [ ] Injection: JwtService
- [ ] Handles missing JWT gracefully
- [ ] Validates signature and expiration
- [ ] Sets authentication context with ROLE_USER
- [ ] Returns 401 for invalid/expired tokens
- [ ] Only applies to /api/v1/account/**

---

### Task 3.12: Create RateLimitFilter

**Description**: Spring Security filter to enforce rate limits on API requests.

**Responsibilities**:
- Extract API key from request
- Check Redis counter
- Return 429 if limit exceeded
- Add rate limit headers (X-RateLimit-*)
- Can be disabled in dev profile

**Acceptance criteria**:
- [ ] Filter created in security/filters/ package
- [ ] Marked as @Component with correct @Order (before auth filters)
- [ ] Injection: RateLimitService, SecurityProperties
- [ ] Skips check if rate limiting disabled (dev profile)
- [ ] Returns 429 Too Many Requests with proper headers
- [ ] Adds X-RateLimit-Limit, X-RateLimit-Remaining headers
- [ ] Adds Retry-After header when limit exceeded
- [ ] Only applies to API key authenticated endpoints

---

## Part D: Controllers (REST Endpoints)

### Task 3.13: Create AuthController

**Description**: REST endpoints for authentication (register, login, refresh, logout).

**Endpoints**:

**POST /api/v1/auth/register** (Public, 201 Created)
- Input: RegisterRequest (username, email, password)
- Output: RegisterResponse (clientId, username)
- Validation: username/email unique, password strength
- Creates client account only (API keys created separately via /account/api-keys)

**POST /api/v1/auth/login** (Public, 200 OK)
- Input: LoginRequest (username, password)
- Output: LoginResponse (accessToken, refreshToken, expiresIn)
- Validates credentials, generates JWT tokens

**POST /api/v1/auth/refresh** (Public, 200 OK)
- Input: RefreshRequest (refreshToken)
- Output: RefreshResponse (accessToken, refreshToken)
- Rotates refresh token (single-use pattern)

**POST /api/v1/auth/logout** (JWT Required, 200 OK)
- Input: JWT in Authorization header
- Effect: Revokes refresh token
- Output: MessageResponse

**Acceptance criteria**:
- [ ] Controller created in controller/ package
- [ ] Marked as @RestController with @RequestMapping("/api/v1/auth")
- [ ] Injection: ClientService, ApiKeyService, JwtService, RefreshTokenService
- [ ] 4 endpoints implemented with correct HTTP methods/status codes
- [ ] Input/output validated with @Valid
- [ ] Delegation to appropriate services
- [ ] Follows REST conventions
- [ ] Exception handling via GlobalExceptionHandler

---

### Task 3.14: Create AccountController

**Description**: REST endpoints for account management (profile, API keys).

**Endpoints**:

**GET /api/v1/account/profile** (JWT Required, 200 OK)
- Output: ProfileResponse (clientId, username, email, totalKeys, createdAt)

**GET /api/v1/account/api-keys** (JWT Required, 200 OK)
- Output: ApiKeyListResponse (list of ApiKeyResponse)
- Shows partial keys: sk_live_abc...xyz

**POST /api/v1/account/api-keys** (JWT Required, 201 Created)
- Input: CreateApiKeyRequest (name, optional)
- Output: ApiKeyResponse (id, name, keyValue - FULL KEY, shown once)

**DELETE /api/v1/account/api-keys/{id}** (JWT Required, 200 OK)
- Effect: Revokes specific API key
- Output: MessageResponse

**Acceptance criteria**:
- [ ] Controller created in controller/ package
- [ ] Marked as @RestController with @RequestMapping("/api/v1/account")
- [ ] Injection: ClientService, ApiKeyService
- [ ] 4 endpoints implemented with correct HTTP methods/status codes
- [ ] Extracts clientId from JWT (Spring Security context)
- [ ] Input/output validated with @Valid
- [ ] Follows REST conventions
- [ ] Only accessible with JWT authentication

---

## Part E: DTOs & Exceptions

### Task 3.15: Create DTOs

**Description**: Classes for API request/response payloads.

**DTOs to create**:

**auth/ package**:
- RegisterRequest (username, email, password)
- RegisterResponse (clientId, username, defaultApiKey)
- LoginRequest (username, password)
- LoginResponse (accessToken, refreshToken, expiresIn)
- RefreshRequest (refreshToken)
- RefreshResponse (accessToken, refreshToken)

**account/ package**:
- ProfileResponse (clientId, username, email, totalKeys, createdAt)
- ApiKeyResponse (id, name, keyValue OR keyPrefix, isActive, createdAt)
- ApiKeyListResponse (List<ApiKeyResponse>)
- CreateApiKeyRequest (name, optional)

**common/ package**:
- MessageResponse (message)

**Acceptance criteria**:
- [ ] All DTOs created in dto/ package with subpackages
- [ ] Validation annotations present (@NotBlank, @Email, @Size, etc.)
- [ ] Can be serialized to/from JSON
- [ ] Use Java records (immutable)

---

### Task 3.16: Create Custom Exceptions

**Description**: Exception classes for authentication/authorization errors.

**Exceptions to create**:

- InvalidApiKeyException (API key not found or inactive) → HTTP 401
- InvalidCredentialsException (wrong username/password) → HTTP 401
- InvalidTokenException (JWT invalid or malformed) → HTTP 401
- TokenExpiredException (JWT expired) → HTTP 401
- DuplicateUsernameException (username already exists) → HTTP 409
- DuplicateEmailException (email already exists) → HTTP 409

**Acceptance criteria**:
- [ ] All exceptions created in exception/ package
- [ ] Extend RuntimeException (unchecked)
- [ ] Have message constructors
- [ ] Can be caught by GlobalExceptionHandler

---

### Task 3.17: Create GlobalExceptionHandler

**Description**: Centralized exception handling for REST API.

**Purpose**: Convert exceptions to appropriate HTTP responses

**Exceptions to handle**:
- InvalidApiKeyException → 401 Unauthorized
- InvalidCredentialsException → 401 Unauthorized
- InvalidTokenException → 401 Unauthorized
- TokenExpiredException → 401 Unauthorized
- DuplicateUsernameException → 409 Conflict
- DuplicateEmailException → 409 Conflict
- MethodArgumentNotValidException → 400 Bad Request
- Generic exceptions → 500 Internal Server Error

**Acceptance criteria**:
- [ ] Handler created in exception/ package
- [ ] Marked as @RestControllerAdvice
- [ ] One @ExceptionHandler method per exception type
- [ ] Returns consistent error response format (message, timestamp, path)
- [ ] Proper HTTP status codes
- [ ] Can be tested independently

---

## Part F: Configuration

### Task 3.18: Create RedisConfig

**Description**: Configuration for Redis connection and template setup.

**Acceptance criteria**:
- [ ] Config class created in config/ package
- [ ] Marked as @Configuration
- [ ] @Bean methods for RedisConnectionFactory and RedisTemplate
- [ ] Uses @Value for host/port from application.yaml
- [ ] Proper serialization setup (String keys/values)
- [ ] No hardcoded values

---

### Task 3.19: Create SecurityProperties

**Description**: Type-safe configuration properties for security settings.

**Properties to expose**:
- `canasta.security.api-key.header-name`
- `canasta.security.api-key.prefix`
- `canasta.security.rate-limit.enabled`
- `canasta.security.rate-limit.max-requests-per-hour`

**Acceptance criteria**:
- [ ] Config class created in config/ package
- [ ] Marked as @Configuration and @ConfigurationProperties("canasta.security")
- [ ] Inner classes for api-key and rate-limit configs
- [ ] Proper defaults
- [ ] Can be injected as dependency
- [ ] Type-safe (no string properties)

---

### Task 3.20: Create JwtProperties

**Description**: Type-safe configuration properties for JWT settings.

**Properties to expose**:
- `canasta.security.jwt.secret`
- `canasta.security.jwt.access-token-ttl` (milliseconds)
- `canasta.security.jwt.refresh-token-ttl` (milliseconds)

**Acceptance criteria**:
- [ ] Config class created in config/ package
- [ ] Marked as @Configuration and @ConfigurationProperties("canasta.security.jwt")
- [ ] Fields: secret, accessTokenTtl, refreshTokenTtl
- [ ] Proper defaults (15 min access, 7 day refresh)
- [ ] Can be injected as dependency
- [ ] Type-safe

---

### Task 3.21: Update SecurityConfig

**Description**: Configure Spring Security for dual authentication.

**What to update**:
- Add JWT + API Key filter chains
- Define authentication rules per endpoint type
- Disable CSRF (stateless API)
- Disable sessions (stateless API)
- Setup exception handling

**Acceptance criteria**:
- [ ] File updated: config/SecurityConfig.java
- [ ] Constructor injection: JwtAuthFilter, ApiKeyAuthFilter, RateLimitFilter
- [ ] @Bean SecurityFilterChain configured
- [ ] CSRF disabled
- [ ] Sessions disabled (STATELESS)
- [ ] Authorization rules correct:
  - [ ] /api/v1/auth/** (except /logout) → permitAll()
  - [ ] /api/v1/account/** → JWT required
  - [ ] /api/v1/products/**, /api/v1/prices/** → API Key required
- [ ] Filters registered in correct order: RateLimit → Jwt → ApiKey
- [ ] Exception handling configured (AuthenticationEntryPoint)

---

### Task 3.22: Update application.yaml

**Description**: Add Redis, JWT, and security configuration.

**To add**:
```yaml
spring.data.redis:
  host: localhost
  port: 6379
  timeout: 2000ms

canasta.security:
  api-key:
    header-name: Authorization
    prefix: "Bearer "
  jwt:
    secret: "${JWT_SECRET:changeme-dev-only-secret-key-at-least-256-bits}"
    access-token-ttl: 900000      # 15 minutes
    refresh-token-ttl: 604800000  # 7 days
  rate-limit:
    enabled: true
    max-requests-per-hour: 100
```

**Acceptance criteria**:
- [ ] File updated: src/main/resources/application.yaml
- [ ] Redis section added
- [ ] JWT section added
- [ ] Security properties added
- [ ] YAML valid (no syntax errors)
- [ ] JWT secret uses environment variable with fallback

---

### Task 3.23: Update application-dev.yaml

**Description**: Disable rate limiting in development profile.

**To add**:
```yaml
canasta.security:
  rate-limit:
    enabled: false
  jwt:
    secret: "dev-secret-key-only-for-development-never-use-in-production"
```

**Acceptance criteria**:
- [ ] File updated: src/main/resources/application-dev.yaml
- [ ] Rate limit disabled
- [ ] JWT secret for development only
- [ ] YAML valid

---

## Part G: Testing & Integration

### Task 3.24: Create Bruno API tests - Registration

**Description**: Create Bruno test for /auth/register endpoint.

**Test scenario**:
- POST to /auth/register with valid credentials
- Verify response: 201 Created, has defaultApiKey field
- Save API key to environment variable for subsequent tests

**Acceptance criteria**:
- [ ] File created: bruno-collection/auth/01-register.bru
- [ ] Request type: POST
- [ ] URL: {{baseUrl}}/api/v1/auth/register
- [ ] Request body: valid JSON with username, email, password
- [ ] Tests: status 201, defaultApiKey present, format validation
- [ ] API key saved to environment variable

---

### Task 3.25: Create Bruno API tests - Login/Refresh/Logout

**Description**: Create Bruno tests for JWT authentication flow.

**Test scenarios**:
- POST /auth/login with valid credentials → save tokens
- POST /auth/refresh with refresh token → save new tokens
- POST /auth/logout with JWT → verify token revoked

**Acceptance criteria**:
- [ ] Files created: bruno-collection/auth/02-login.bru, 03-refresh.bru, 04-logout.bru
- [ ] Login: saves accessToken and refreshToken to environment
- [ ] Refresh: uses refreshToken, saves new tokens
- [ ] Logout: uses accessToken, verifies 200 OK
- [ ] Tests verify response structure and status codes

---

### Task 3.26: Create Bruno API tests - Account Management

**Description**: Create Bruno tests for /account endpoints.

**Test scenarios**:
- GET /account/profile with JWT
- GET /account/api-keys with JWT
- POST /account/api-keys with JWT → save new key
- DELETE /account/api-keys/{id} with JWT

**Acceptance criteria**:
- [ ] Files created: bruno-collection/account/*.bru
- [ ] All requests use JWT from environment ({{accessToken}})
- [ ] Profile test: verifies clientId, username, email
- [ ] List keys test: verifies array response with partial keys
- [ ] Create key test: saves full key to environment
- [ ] Delete key test: verifies 200 OK

---

### Task 3.27: Update Bruno API tests - Data Endpoints

**Description**: Update existing product endpoint tests to use API key authentication.

**Updates needed**:
- GET /api/v1/products → Add Authorization header with API key
- GET /api/v1/products/{id} → Add Authorization header
- GET /api/v1/products/search → Add Authorization header

**Acceptance criteria**:
- [ ] All product endpoints have Authorization header
- [ ] Use {{apiKey}} environment variable
- [ ] Tests pass with valid API key
- [ ] Tests fail (401) without API key

---

### Task 3.28: Integration Testing

**Description**: Start Spring Boot and verify dual authentication system works end-to-end.

**Manual verification checklist**:
- [ ] Spring Boot starts without errors (mvn spring-boot:run -Dspring-boot.run.profiles=dev)
- [ ] Redis connected (logs show connection)
- [ ] PostgreSQL migrations applied (3 migrations total)
- [ ] Tables exist: clients, api_keys, refresh_tokens
- [ ] POST /auth/register returns 201 with defaultApiKey
- [ ] POST /auth/login returns 200 with JWT tokens
- [ ] POST /auth/refresh returns 200 with new tokens
- [ ] GET /account/profile with JWT returns 200
- [ ] POST /account/api-keys with JWT returns 201 with full key
- [ ] GET /products without Authorization returns 401
- [ ] GET /products with valid API key returns 200
- [ ] GET /products with JWT returns 401 (wrong auth type)
- [ ] GET /account/profile with API key returns 401 (wrong auth type)
- [ ] POST /auth/logout revokes refresh token
- [ ] Redis cache populated (verify with redis-cli)

**Commands**:
```bash
# Start backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Check Redis
docker exec -it canasta-redis redis-cli
> KEYS *
> GET api_key:sk_live_...

# Check PostgreSQL
docker exec -it canasta-postgres psql -U canasta -d canasta
> \dt
> SELECT * FROM api_keys;
> SELECT * FROM refresh_tokens;
```

---

### Task 3.29: Rate Limiting Verification

**Description**: Test rate limiting by making >100 requests.

**Test scenario**:
- Register new client
- Make 101 requests with same API key
- First 100 return 200
- 101st returns 429

**Acceptance criteria**:
- [ ] Rate limiting enabled (set in application.yaml temporarily)
- [ ] First 100 requests succeed
- [ ] 101st request returns 429 Too Many Requests
- [ ] Response includes rate limit headers (X-RateLimit-*)
- [ ] Redis counter increments (verify with redis-cli)

---

### Task 3.30: (Optional) Create Unit Tests

**Description**: Unit tests for critical components (if time permits).

**Test classes to create**:
- ClientServiceTest
- ApiKeyServiceTest
- JwtServiceTest
- RefreshTokenServiceTest
- RateLimitServiceTest
- ApiKeyGeneratorTest
- JwtUtilTest
- AuthControllerTest
- AccountControllerTest

**Acceptance criteria**:
- [ ] Tests created for all new components
- [ ] Use mocks for dependencies (Mockito)
- [ ] Use embedded Redis for RateLimitService
- [ ] Use H2 database for repository tests
- [ ] Coverage >70%
- [ ] All tests pass

---

## Final Verification Checklist

| Component | Status | Details |
|-----------|--------|---------|
| **Database** | ⏳ Pending | Migration V3 applied, tables created |
| **Entities** | ⏳ Pending | ApiKey, RefreshToken entities |
| **Utilities** | ⏳ Pending | ApiKeyGenerator, JwtUtil |
| **Services** | ⏳ Pending | Client, ApiKey, Jwt, RefreshToken, RateLimit |
| **Filters** | ⏳ Pending | ApiKeyAuth, JwtAuth, RateLimit |
| **Controllers** | ⏳ Pending | Auth (4 endpoints), Account (4 endpoints) |
| **DTOs** | ⏳ Pending | 11 records created |
| **Exceptions** | ⏳ Pending | 6 custom exceptions + handler |
| **Configuration** | ⏳ Pending | Redis, Security, JWT properties |
| **YAML** | ⏳ Pending | Redis + JWT + security properties |
| **Bruno Tests** | ⏳ Pending | Auth + account + data endpoints |
| **Integration** | ⏳ Pending | End-to-end flow verified |
| **Spring Boot** | ⏳ Pending | Starts, Redis + DB connected |
| **Rate Limit** | ⏳ Pending | 429 after 100 requests |
| **Dual Auth** | ⏳ Pending | JWT for /account, API Key for /products |

---

## Key Learning Points to Understand

### API Keys
1. **Cryptographic security** - SecureRandom, entropy, token format
2. **Multiple keys per user** - 1-to-many relationship pattern
3. **Partial key display** - Security best practice for dashboards
4. **Soft delete with audit** - revoked_at timestamp

### JWT
5. **Access vs Refresh tokens** - Different lifetimes, different purposes
6. **JWT structure** - Header, payload, signature (HS256)
7. **Stateless validation** - No DB lookup for access tokens
8. **Refresh token rotation** - Single-use pattern prevents replay attacks
9. **Token expiration** - Why short-lived access tokens are important

### Spring Security
10. **Multiple authentication filters** - Order matters
11. **Authentication context** - How Spring tracks authenticated user
12. **Stateless sessions** - No cookies, no server-side sessions
13. **Authorization rules** - Per-endpoint auth requirements

### Architecture
14. **Dual authentication** - Different auth for different use cases
15. **Cache-aside pattern** - Redis caching for performance
16. **Rate limiting algorithms** - Sliding window with Redis
17. **Soft delete patterns** - Audit trails and data retention

---

**Next phase**: 04-backend-business-logic (Analytics endpoints, DTOs, Advanced queries)

---

**Last Updated**: 2026-02-07
