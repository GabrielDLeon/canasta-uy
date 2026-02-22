---
status: core_complete
updated: 2026-02-22
---

# Phase 3: Backend - Authentication & Security - CONTEXT

---

## Objective

Implement dual authentication system for CanastaUY API:
- **JWT authentication** for web dashboard and account management
- **API Key authentication** for programmatic data access (scripts, notebooks, integrations)

**Status**: CORE COMPLETE - Implementation finished, pending testing phase

---

## Key Concepts

### Dual Authentication Strategy

**CanastaUY implements two authentication methods for different use cases:**

#### 1. JWT Authentication (Web Dashboard)
**Use case**: Human users managing their account via web interface
- Login with email/password → receive JWT tokens
- Access token (short-lived, 15 minutes)
- Refresh token (long-lived, 7 days)
- Used for account management endpoints

**Note**: Username field deprecated. Email is the primary identifier.

**Why JWT for web:**
- ✅ Standard for web authentication
- ✅ Automatic expiration (better security)
- ✅ Stateless validation (no DB lookup per request)
- ✅ Suitable for browser-based sessions

#### 2. API Key Authentication (Programmatic Access)
**Use case**: Scripts, notebooks, data analysis tools, integrations
- Register once → receive API key
- Long-lived tokens (don't expire automatically)
- Used for data access endpoints (products, prices)

**Why API Keys for programmatic access:**
- ✅ Zero friction (copy key, start using)
- ✅ No token refresh logic in scripts
- ✅ Perfect for batch jobs and automation
- ✅ Industry standard (GitHub, Stripe, AWS)

---

### API Key Design

**Format**: `sk_live_<64_hex_characters>`
- `sk_` prefix: Identifies as secret key
- `live_` environment: Distinguishes from `test_`
- 64 hex chars: 256-bit entropy (cryptographically secure)

**Key characteristics:**
- Multiple keys per client (production, development, CI/CD)
- Each key has optional name ("My laptop", "GitHub Actions")
- Shown only once at creation (user must save it)
- Can be revoked individually (soft delete)
- Never expires automatically
- Tracks last_used_at for auditing

### JWT Design

**Access Token (short-lived):**
- Expires in 15 minutes
- Contains: clientId, username, roles
- Signed with HS256 (HMAC SHA-256)
- Used for account management endpoints

**Refresh Token (long-lived):**
- Expires in 7 days
- Stored in database (can be revoked)
- Single-use with rotation (new refresh token on each use)
- Used to obtain new access tokens

**Token Flow:**
1. Login → receive access + refresh tokens
2. Use access token for requests
3. When access expires → use refresh to get new access
4. Logout → revoke refresh token

---

### Rate Limiting [NICE TO HAVE]

**Priority**: Implement after core authentication is complete.

**Purpose**: Prevent API abuse by limiting requests per API key.

**Target**: 100 requests/hour per API key, return HTTP 429 when exceeded.

**Note**: This security layer will be implemented in a later phase of the project.

---

### Security Layers (Defense in Depth)

1. **Authentication** (Who are you?)
   - Verify API key exists and belongs to an active client
   - Cache result to avoid DB hit on every request
   - Return 401 Unauthorized if invalid/revoked

2. **Authorization** (What can you do?)
   - All authenticated users have same permissions (read-only)
   - No roles/permissions needed yet (Phase 4 might add admin endpoints)
   - Everyone can access `/api/v1/products`, `/api/v1/prices`, etc.

3. **Rate Limiting** (How much can you use?)
   - Prevent abuse and unfair usage
   - Protect backend from overload
   - Return 429 Too Many Requests when exceeded

4. **Audit Trail** (What happened?)
   - Log all authentication attempts (success/failure)
   - Track which IP/key accessed which endpoint
   - Helps detect suspicious patterns

---

### Registration Model

**Open registration** (vs. invite-only):
- Any person can create account and get API key
- Lower friction = more usage = more feedback
- Public dataset doesn't require strict access control
- Risk of spam is low (only need valid email eventually)

**What happens during registration:**
1. User provides email, password (username field deprecated)
2. System validates uniqueness (no duplicate emails)
3. Password is hashed (never stored in plaintext)
4. Client record saved to PostgreSQL (email stored in username field for DB compatibility)
5. User must create API key via /account/api-keys endpoint
6. API key returned to user (only shown once!)
7. User must save the key for future API calls

---

## Architecture Overview

### Request Flow

#### **Flow A: JWT Authentication (Account Management)**

```
Client sends request with JWT access token
    ↓
JwtAuthFilter: Validate JWT token
    ├─ Parse and verify signature
    ├─ Check expiration
    ├─ Invalid/expired → Return HTTP 401
    └─ Valid → Set Authentication context
         ↓
    Spring Security: Grant ROLE_USER permission
         ↓
    Controller processes request (account management)
         ↓
    Return JSON response
```

#### **Flow B: API Key Authentication (Data Access)**

```
Client sends request with API Key in header
    ↓
ApiKeyAuthFilter: Validate API key exists + active
         ├─ Check Redis cache first (fast)
         │   ├─ Cache miss → Query PostgreSQL
         │   └─ Cache result for 1 hour
         ├─ Invalid/revoked → Return HTTP 401
         └─ Valid & active → Continue
              ↓
         Spring Security: Grant ROLE_USER permission
              ↓
         Controller processes request (data access)
              ↓
         Return JSON response
```

### Data Storage

**PostgreSQL**:
- `clients` table: User accounts (username, email, password_hash, is_active)
- `api_keys` table: API keys (client_id, key_value, name, is_active, last_used_at)
- `refresh_tokens` table: JWT refresh tokens (client_id, token_value, expires_at, is_revoked)

**Redis**:
- API Key cache: `api_key:<key>` → client data (TTL 1h)
- Rate limit counters: `rate_limit:<key>:<hour>` → count (TTL 1h)
- JWT blacklist (optional): `jwt_blacklist:<token>` → revoked access tokens

---

## Design Decisions

### 1. Dual Authentication (JWT + API Keys)
**Chosen**: Both JWT and API Keys
**Reason**: Different use cases require different auth mechanisms
**JWT for**: Web dashboard, account management, human interaction
**API Keys for**: Scripts, notebooks, data analysis, automation
**Trade-off**: More complexity, but better UX for each use case
**Impact**: Flexible authentication, industry-standard approach

---

### 2. Open Registration
**Chosen**: Anyone can POST /auth/register
**Reason**: Public data, encourage adoption
**Alternative**: Invite-only (ADMIN creates clients)
**Trade-off**: Potential for spam accounts
**Impact**: No bottleneck for new users

---

### 3. Multiple API Keys per Client
**Chosen**: 1-to-many relationship (clients → api_keys)
**Reason**: Users need different keys for different environments/purposes
**Use cases**: Production key, development key, CI/CD key, laptop key
**Alternative**: Single key per user (limited, poor UX)
**Trade-off**: Slightly more complex data model
**Impact**: Professional UX, key rotation without downtime

---

### 4. Rate Limiting with Redis
**Chosen**: 100 requests per hour per API key (conservative)
**Reason**: Protects against abuse while remaining friendly for data science workflows
**Alternative**: PostgreSQL-based (slower), no limit (vulnerable), higher limits
**Trade-off**: Requires Redis to be running; may require rate limit increase for future frontend
**Impact**: Protects backend from abuse, forces users to optimize queries, fair usage

---

### 5. Password Hashing (BCrypt)
**Chosen**: BCrypt with strength 12
**Reason**: Industry standard, resistant to brute-force attacks
**Alternative**: Plaintext (bad), MD5/SHA (crackable), Argon2 (newer)
**Trade-off**: Slightly slower (by design, to deter brute-force)
**Impact**: Passwords safe even if database stolen

---

### 6. API Key Visibility
**Chosen**: Show full key only once at creation
**Reason**: Security best practice (prevents accidental leaks)
**Dashboard shows**: Partial key only (sk_live_abc...xyz)
**Alternative**: Always retrievable (security risk)
**Trade-off**: User must save key immediately
**Impact**: Encourages secure handling, industry standard

### 7. JWT Storage Strategy
**Chosen**: Refresh tokens in database, access tokens stateless
**Reason**: Balance between revocation control and performance
**Access tokens**: Self-contained, validated by signature (no DB lookup)
**Refresh tokens**: Stored in DB, can be revoked immediately
**Trade-off**: Revoked access tokens valid until expiration (15 min max)
**Impact**: Fast validation, controllable revocation

---

### 8. Soft Delete for Revoked Keys
**Chosen**: Set `is_active = false` + `revoked_at` timestamp
**Reason**: Preserve audit trail, historical data
**Alternative**: Hard delete (loses information)
**Trade-off**: `is_active` column + timestamp required
**Impact**: Can see when/why keys were revoked, audit compliance

### 9. Refresh Token Rotation
**Chosen**: Single-use refresh tokens with rotation
**Reason**: Prevents token replay attacks
**How**: Each refresh generates new access + new refresh token
**Alternative**: Reusable refresh tokens (less secure)
**Trade-off**: Slightly more complex logic
**Impact**: Better security against stolen refresh tokens

---

## Endpoints to Implement

### Authentication Endpoints (Public)

**POST /api/v1/auth/register**
- Public endpoint (no authentication required)
- Input: email, password (username deprecated)
- Output: clientId, email
- Response: HTTP 201 Created
- Note: API key must be created separately via POST /account/api-keys

**POST /api/v1/auth/login**
- Public endpoint (no authentication required)
- Input: username, password
- Output: accessToken (JWT, 15 min), refreshToken (7 days)
- Response: HTTP 200 OK

**POST /api/v1/auth/refresh**
- Public endpoint (no authentication required)
- Input: refreshToken
- Output: new accessToken, new refreshToken (rotation)
- Response: HTTP 200 OK

**POST /api/v1/auth/logout**
- Requires JWT access token
- Effect: Revokes refresh token
- Response: HTTP 200 OK

### Account Management Endpoints (JWT Required)

**GET /api/v1/account/profile**
- Requires JWT access token
- Returns: clientId, username, email, totalKeys, createdAt
- Response: HTTP 200 OK

**GET /api/v1/account/api-keys**
- Requires JWT access token
- Returns: List of user's API keys (partial key shown: sk_live_abc...xyz)
- Response: HTTP 200 OK

**POST /api/v1/account/api-keys**
- Requires JWT access token
- Input: name (optional, e.g., "Production key")
- Output: id, name, keyValue (full key shown ONCE)
- Response: HTTP 201 Created

**DELETE /api/v1/account/api-keys/{id}**
- Requires JWT access token
- Effect: Revokes specific API key
- Response: HTTP 200 OK

### Data Access Endpoints (API Key Required)

All data endpoints require API key in Authorization header:
- GET /api/v1/products
- GET /api/v1/products/{id}
- GET /api/v1/products/search
- GET /api/v1/prices/{productId}
- etc.

---

## Security Considerations

### What We're Protecting Against

**Authentication attacks:**
- Unauthorized access without API key → Rejected
- Forged/manipulated keys → Validated in Redis/DB
- Revoked keys → Checked is_active flag

**Rate limit abuse:**
- One client hogging all resources → Limited to 1000/hour
- DDoS attacks → Each request counted, rejected if over limit
- Accidental bugs (infinite loops) → Protected by rate limit

**Password attacks:**
- Password leaks from database → Hashed with BCrypt, unrecoverable
- Brute-force attacks → BCrypt strength 12 (slow by design)
- Rainbow tables → BCrypt uses salt, defeats tables

### What We're NOT Protecting Against

**Not in scope for Phase 3:**
- HTTPS/TLS encryption (production deployment concern)
- IP whitelisting (enterprise feature, Phase 5+)
- API key rotation (Phase 4+)
- Two-factor authentication (not needed for read-only API)
- Detailed usage analytics (Phase 4+)

---

## Configuration

### Environment-specific Settings

**Production (application.yaml)**:
- Rate limiting: Enabled (100 requests/hour)
- Redis: Production instance
- HTTPS: Required (enforced at load balancer)

**Development (application-dev.yaml)**:
- Rate limiting: Disabled (easier testing)
- Redis: Local instance (docker-compose)
- HTTPS: Not enforced

---

## Common Challenges & Solutions

### Challenge: API Key Exposed in Code
**Problem**: Key committed to GitHub, exposed in logs
**Solution**: Mask key in logs, use environment variables
**Best practice**: Keys stored in secure vaults, not in code

### Challenge: Rate Limit False Positives
**Problem**: Legitimate user hits limit due to retry logic
**Solution**: Make limit generous (1000/hour = ~3 per second)
**Alternative**: Add IP-based limits for registration endpoint

### Challenge: Redis Becomes Bottleneck
**Problem**: Rate limit checks slower than API responses
**Solution**: Redis is O(1), negligible overhead (~0.1ms)
**Monitoring**: Track Redis latency, scale if needed

### Challenge: Revoked Key Takes Time to Reflect
**Problem**: After revocation, cached key still valid for 1 hour
**Solution**: Accept 1-hour delay, or clear cache immediately on revoke
**Trade-off**: Immediate revocation = more DB hits, slower

---

## Performance Impact

### Redis Caching Benefits

**Without cache**:
- Every API request hits PostgreSQL
- ~5-10ms latency per request
- DB becomes bottleneck with 1000s of concurrent users

**With cache** (1h TTL):
- First request hits DB (~5-10ms)
- Subsequent requests hit Redis (~0.1ms)
- Cache hit rate >95% expected
- 50-100x faster authentication

### Rate Limit Overhead

**Redis INCR operation**: O(1) = ~0.1ms
**Compared to API request**: ~50-200ms (depends on query)
**Impact**: Negligible (<1% overhead)

---

## Migration Path

### Phase 3 Scope (Current)
- API Key authentication
- Rate limiting (1000/hour)
- Soft delete revocation

### Phase 4+ (Future, not in Phase 3)
- Email verification (optional)
- Admin role + management endpoints
- API key rotation
- Usage analytics dashboard
- IP whitelisting (enterprise)
- Custom rate limits per client

---

## References

- `PLAN.md` - Implementation roadmap
- `TASKS.md` - Task checklist
- `../02-backend-domain-persistence/` - Domain layer (Phase 2)
- `../01-backend-infrastructure/` - Database schema (Phase 1)
- `../../roadmap-backend.md` - Overall backend roadmap

---

**Last Updated**: 2026-02-07
