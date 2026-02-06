# Backend Roadmap - CanastaUY

**Stack**: Spring Boot 4.0.2 + Java 21 + PostgreSQL 15 + Redis 7.x + Flyway

---

## Implementation Phases

### Phase 1: Infrastructure (COMPLETED)
Docker + PostgreSQL + Flyway + imported data (774,716 records)

**See**: `01-backend-infrastructure/`

---

### Phase 2: Domain & Persistence (IN PROGRESS)
Domain layer (JPA entities) and persistence (repositories)

**Tasks**:
- [ ] Create JPA entities (Category, Product, Price, Client)
- [ ] Create repositories (Spring Data JPA)
- [ ] Basic tests

**See**: `02-backend-domain-persistence/`

---

### Phase 3: Security & Auth
JWT authentication and authorization

**Tasks**:
- [ ] JWT utilities
- [ ] Spring Security config
- [ ] Auth endpoints (register, login)
- [ ] API Key validation
- [ ] Rate limiting with Redis

---

### Phase 4: Business Logic & Services
Business logic (ProductService, PriceService, AnalyticsService)

**Tasks**:
- [ ] DTOs and Mappers
- [ ] Services with validations
- [ ] Global exception handling
- [ ] Unit tests

---

### Phase 5: API Controllers
REST controllers for all endpoints

**Tasks**:
- [ ] ProductController
- [ ] PriceController
- [ ] AnalyticsController
- [ ] AuthController
- [ ] Integration tests

**Main endpoints**:
```
GET    /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products/{id}/prices
GET    /api/v1/analytics/average-price-by-category
GET    /api/v1/analytics/price-trend/{productId}
GET    /api/v1/analytics/inflation-rate-by-category
```

---

### Phase 6: Caching & Performance
Optimization with Redis and performance analysis

**Tasks**:
- [ ] Redis cache config
- [ ] @Cacheable decorators
- [ ] Cache invalidation
- [ ] Performance testing

---

### Phase 7: Documentation & Polish
Final documentation and production readiness

**Tasks**:
- [ ] Swagger/OpenAPI config
- [ ] Complete README
- [ ] Complete error handling
- [ ] Structured logging

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
```

---

## Tech Stack

- **Spring Boot 4.0.2** + Java 21 + Maven
- **PostgreSQL 15** + Flyway + Spring Data JPA
- **Redis 7.x** + Spring Cache
- **Spring Security** + JWT
- **Swagger** (springdoc-openapi)
- **JUnit 5** + TestContainers
- **SLF4J** + Logback

---

## Documentation by phase

 Phase  Location 
-----------------
 Phase 1  `01-backend-infrastructure/` 
 Phase 2  `02-backend-domain-persistence/` 
 Phase 3  `03-backend-auth-security/` 
 Phases 4-7  (To be created) 
