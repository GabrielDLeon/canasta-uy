---
status: completed
updated: 2026-02-06
---

# Phase 2: Backend - Domain & Persistence - CONTEXT

---

## Objective ✅

Create the domain layer (JPA entities) and persistence layer (repositories) so Spring Boot can read/write data to PostgreSQL using Java objects.

**Status**: COMPLETED - Spring Boot now has full access to 774,716 price records through a functional REST API.

---

## Key Concepts

### JPA (Java Persistence API)
- Java specification for ORM (Object-Relational Mapping)
- Hibernate is the implementation Spring Boot uses
- Converts Java objects ↔ SQL table rows

### Spring Data JPA
- Layer on top of JPA that automatically generates queries
- `JpaRepository<Entity, ID>` provides free CRUD
- Naming conventions: `findByFieldName()` generates automatic query

### JPA Entities
- Annotated Java classes that map to database tables
- @Entity, @Table, @Id, @Column, etc.
- Can have relationships (@ManyToOne, @OneToMany)

### Lazy Loading
- `FetchType.LAZY` doesn't automatically load relationships
- Pro: Better performance (fewer queries)
- Con: `LazyInitializationException` if accessed outside transaction

---

## Architecture

### Domain layer (model/)
```
Category  (1:N)  Product  (1:N)  Price
```

### Persistence layer (repository/)
```
CategoryRepository: CRUD for categories
ProductRepository: CRUD + searches for products
PriceRepository: CRUD for prices (composite key)
ClientRepository: CRUD for clients (Phase 3)
```

### Presentation layer (controller/)
```
ProductController: Basic REST endpoints
- GET /api/v1/products
- GET /api/v1/products/{id}
```

---

## Entities to Create

### 1. Category.java
- Maps `categories` table
- PK: category_id (auto-increment)
- Columns: name, created_at
- Relationship: 1:N  Product

### 2. Product.java
- Maps `products` table
- PK: product_id (manual)
- Columns: name, specification, brand, created_at, updated_at
- Relationship: N:1  Category (lazy loading)
- Relationship: 1:N  Price

### 3. Price.java
- Maps `prices` table
- Composite PK: (product_id, date)
- Columns: price_min, price_max, price_avg, price_median, price_std, store_count, offer_count, offer_percentage
- Relationship: N:1  Product (lazy loading)
- Note: Requires @IdClass(PriceId.class)

### 4. Client.java
- Maps `clients` table (empty for now)
- PK: client_id (auto-increment)
- Columns: username, email, password, api_key, is_active, created_at, updated_at
- Will be used in Phase 3 (Auth)

---

## Design Decisions

### Lazy Loading in relationships
- Chosen: FetchType.LAZY in @ManyToOne
- Reason: Better performance (doesn't load automatically)
- Alternative: EAGER (loads automatically)
- Note: In controllers, use @Transactional or DTOs

### Composite Key in Price
- Used: @EmbeddedId (PriceId.java)
- Alternative: @IdClass (simpler but less OOP)
- Reason: More professional, OOP approach, cohesive key object
- Implementation: PriceId is @Embeddable and implements Serializable

### BigDecimal for prices
- Mandatory: NEVER use double/float
- Reason: Floating-point precision errors
- BigDecimal guarantees decimal accuracy

---

## Common Problems

### LazyInitializationException
```
Error: could not initialize proxy - no Session
```
Cause: Access @ManyToOne LAZY relationship outside transaction

Solution:
```java
@Transactional(readOnly = true)
public Product getProductWithCategory(Integer id) {
    Product p = productRepository.findById(id).orElseThrow();
    p.getCategory().getName(); // Force load
    return p;
}
```

### Infinite recursion in JSON (RESOLVED)
```
Error: StackOverflowError + hibernateLazyInitializer in response
```
Cause: Product ↔ Category (bidirectional relationship)

Solution Implemented:
```java
@JsonIgnore  // In Category.java
@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
private List<Product> products;
```
Result: Clean JSON responses without circular references

### N+1 queries problem
```
1 query for products
N queries for each category (problem!)
```
Solution:
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();
```

---

## Artifacts Created

### Entities (model/)
- **Category.java**: Maps categories table (134 records)
- **Product.java**: Maps products table (306 records)
- **Price.java**: Maps prices table (774,716 records) with @EmbeddedId
- **PriceId.java**: Composite key @Embeddable class
- **Client.java**: Maps clients table (for Phase 3 auth)

### Repositories (repository/)
- **CategoryRepository**: findByName(), findAllByOrderByNameAsc()
- **ProductRepository**: findByCategory(), findByBrand(), findByNameContainingIgnoreCase(), searchByNameKeyword()
- **PriceRepository**: findByIdProductId(), findByIdDateBetween(), findByIdProductIdAndIdDateBetween()
- **ClientRepository**: findByUsername(), findByEmail(), findByApiKey()

### Services (service/)
- **ProductService**: getAllProducts(), getProductById(), searchProducts()
- **PriceService**: getPricesByProductId(), getPricesByDateRange(), getPricesByProductAndDateRange()
- **CategoryService**: getAllCategories(), getCategoryByName()

### Controllers (controller/)
- **ProductController**:
  - GET /api/v1/products → List of all products
  - GET /api/v1/products/{id} → Single product or 404
  - GET /api/v1/products/search?query=X → Search by name (LIKE)

### Configuration (config/)
- **SecurityConfig**: Disables Spring Security for development (allows all requests)

---

## Tested Endpoints

All endpoints verified and returning correct JSON:

```bash
# Get all products (306 records)
curl http://localhost:8080/api/v1/products

# Get specific product
curl http://localhost:8080/api/v1/products/15
# Response: {"productId":15,"name":"Arroz blanco Blue Patna Bolsa 1 kg.","category":{...}}

# Search products
curl "http://localhost:8080/api/v1/products/search?query=arroz"
# Response: Array of 6 rice products
```

---

## References

- `PLAN.md` - Implementation details with code
- `TASKS.md` - Task checklist
- `database-schema.md` - PostgreSQL schema (Phase 1)
