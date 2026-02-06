---
status: in-progress
updated: 2026-02-06
---

# Phase 2: Backend - Domain & Persistence - PLAN

**Objective**: Create domain layer (JPA entities) and persistence layer (repositories) to access data from Spring Boot.

---

## Implementation Plan

### 1. Create JPA entities

Entities map PostgreSQL tables to Java objects that Hibernate can manipulate.

**Category.java**: Maps `categories` table
- Auto-increment PK
- Field: name (UNIQUE)
- Relationship: 1:N  Product (lazy loading)

**Product.java**: Maps `products` table
- Manual PK
- Fields: name, specification, brand, timestamps
- Relationship: N:1  Category (lazy loading)

**Price.java + PriceId.java**: Maps `prices` table with composite key
- `PriceId`: embeddable class implementing Serializable
- `Price`: entity with `@IdClass(PriceId.class)`
- Fields: price_min, price_max, price_avg, price_median, price_std, store_count, offer_count, offer_percentage
- **Important**: Use BigDecimal (NEVER double/float for decimal precision)

**Client.java**: Maps `clients` table
- Auto-increment PK
- Fields: username, email, password, api_key, is_active, timestamps
- Will be used in Phase 3 for authentication

All use Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor) to reduce boilerplate.

### 2. Create repositories

Repositories extend `JpaRepository<Entity, ID>` which generates CRUD automatically.

**CategoryRepository**: Category searches
- `findByName()`: search by name
- `findAllByOrderByNameAsc()`: list ordered

**ProductRepository**: Product searches
- `findByCategory()`: products of a category
- `findByBrand()`: products of a brand
- `findByNameContainingIgnoreCase()`: partial search
- `@Query searchByName()`: flexible search with LIKE

**PriceRepository**: Price history
- `findByProductId()`: all prices of a product
- `findByProductIdOrderByDateDesc()`: ordered by date
- `findByDateBetween()`: temporal range
- `findByProductIdAndDateBetween()`: product + temporal range

**ClientRepository**: For authentication (Phase 3)
- `findByUsername()`, `findByEmail()`, `findByApiKey()`

### 3. Create basic ProductController

REST controller with 3 endpoints for testing:

**GET /api/v1/products**: Returns list of all products in JSON
**GET /api/v1/products/{id}**: Returns product or 404
**GET /api/v1/products/search?query=X**: Search by name

### 4. Verify everything works

Start Spring Boot:
- No compilation errors
- No database connection errors
- Hibernate validates entities match schema

Test endpoints:
- GET /api/v1/products returns JSON array with all products
- GET /api/v1/products/ID returns JSON of specific product
- JSON structure includes all fields

### 5. (Optional) Create basic tests

Tests with @DataJpaTest for repositories
Tests with @WebMvcTest for controller
Coverage > 70%

---

## Design Decisions

### FetchType.LAZY in relationships
`@ManyToOne(fetch = FetchType.LAZY)` to not automatically load relationships. Pros: better performance. Cons: risk of LazyInitializationException. Solution: use @Transactional or DTOs.

### BigDecimal for prices
Mandatory. NEVER double/float (precision errors). BigDecimal guarantees decimal accuracy.

### @IdClass vs @EmbeddedId
Choose @IdClass for Price because it's simpler. Alternative: @EmbeddedId (more OOP).

### Organized packages
- `model/`: only JPA entities
- `repository/`: only Repository interfaces
- `controller/`: only REST controllers
- Later: `service/`, `dto/`, etc.

---

## Package structure

```
src/main/java/uy/eleven/canasta/
── model/
   ── Category.java
   ── Product.java
   ── Price.java
   ── PriceId.java
   ── Client.java

── repository/
   ── CategoryRepository.java
   ── ProductRepository.java
   ── PriceRepository.java
   ── ClientRepository.java

── controller/
    ── ProductController.java
```

---

## Expected validations

1. Compile without errors
2. Spring Boot starts without schema mismatch
3. GET /api/v1/products returns JSON array
4. GET /api/v1/products/{id} returns product JSON
5. GET /api/v1/products/search returns search results

---

## References

- `CONTEXT.md` - JPA concepts and decisions
- `TASKS.md` - Detailed checklist
- `../01-backend-infrastructure/database-schema.md` - PostgreSQL schema (Phase 1)
