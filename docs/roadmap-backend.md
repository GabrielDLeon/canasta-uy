# Backend Roadmap - CanastaUY

**Stack**: Spring Boot 4.0.2 + Java 21 + PostgreSQL 15 + Redis 7.x + Flyway

---

## Implementation Phases

### Phase 1: Infrastructure (COMPLETED)
Docker + PostgreSQL + Flyway + imported data (774,716 records)

**See**: `01-backend-infrastructure/`

---

### Phase 2: Domain & Persistence (COMPLETED)
Domain layer (JPA entities) and persistence (repositories)

**Status**: Complete - All entities and repositories implemented

**See**: `02-backend-domain-persistence/`

---

### Phase 3: Security & Auth (COMPLETED)
JWT authentication and API Key authorization with Redis caching

**Status**: Complete - Dual authentication working (JWT + API Keys)

**See**: `03-backend-auth-security/`

---

### Phase 4: Business Logic & Analytics (COMPLETED)
Complete API with prices, categories, and analytics

**Tasks**:
- [x] Prices endpoints (/products/{id}/prices, /prices)
- [x] Categories endpoints (/categories/{id}/products, /categories/{id}/stats)
- [x] Analytics endpoints (trend, inflation, compare, top-changes)
- [x] Redis caching for performance
- [ ] Bruno API tests for prices and categories (optional)

**Main endpoints**:
```
GET    /api/v1/products/{id}/prices
GET    /api/v1/prices
GET    /api/v1/categories/{id}/products
GET    /api/v1/categories/{id}/stats
GET    /api/v1/analytics/trend/{productId}
GET    /api/v1/analytics/inflation/{categoryId}
GET    /api/v1/analytics/compare
GET    /api/v1/analytics/top-changes
```

**See**: `04-backend-business-logic/`

---

### Phase 5: Polish & Production (Optional)
Final optimizations and production readiness

**Tasks**:
- [ ] SSL/HTTPS configuration
- [ ] Comprehensive unit testing
- [ ] Monitoring and alerting
- [ ] Performance optimization
- [ ] Final documentation

**Note**: Phase 5 optional - core functionality complete after Phase 4

---

## Clean Architecture

```
uy.eleven.canasta/
── model/                    # JPA Entities
── repository/               # Spring Data JPA
── service/                  # Business Logic
── controller/               # REST Controllers
── dto/                      # Data Transfer Objects
── config/                   # Configurations
── exception/                # Exception Handling
── security/                 # Authentication Filters
```

---

## Tech Stack

- **Spring Boot 4.0.2** + Java 21 + Maven
- **PostgreSQL 15** + Flyway + Spring Data JPA
- **Redis 7.x** + Spring Cache
- **Spring Security** + JWT + API Keys
- **Swagger** (springdoc-openapi)
- **JUnit 5** + TestContainers
- **SLF4J** + Logback

---

## Documentation by phase

| Phase | Status | Location |
|-------|--------|----------|
| Phase 1 | Complete | `01-backend-infrastructure/` |
| Phase 2 | Complete | `02-backend-domain-persistence/` |
| Phase 3 | Complete | `03-backend-auth-security/` |
| Phase 4 | Complete (Core) | `04-backend-business-logic/` |
| Phase 5 | Optional | (Future) |

---

**Last Updated**: 2026-02-28
