---
status: completed
updated: 2026-02-06
---

# Phase 2: Backend - Domain & Persistence - TASKS

---

## Task 2.1: Create Category.java entity ✅

**Description**: Map `categories` table to JPA entity.

**Acceptance criteria**:
- [x] File created
- [x] @Entity, @Table(name = "categories") present
- [x] @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor present (Lombok)
- [x] categoryId property with @Id, @GeneratedValue(strategy = GenerationType.IDENTITY)
- [x] name property with @Column(nullable = false, unique = true)
- [x] createdAt property with @CreationTimestamp
- [x] @OneToMany(mappedBy = "category") with @JsonIgnore to prevent circular references
- [x] Compiles without errors

**Notes**: Used Integer for categoryId (SERIAL maps to int, not long)

---

## Task 2.2: Update Product.java entity ✅

**Description**: Map `products` table.

**Acceptance criteria**:
- [x] Annotations @Entity, @Table(name = "products")
- [x] productId property with @Id (NO @GeneratedValue - IDs come from data)
- [x] Properties: name, specification, brand with @Column and correct lengths
- [x] category property with @ManyToOne(fetch = FetchType.LAZY)
- [x] Timestamp properties with @CreationTimestamp, @UpdateTimestamp
- [x] Compiles without errors

**Notes**: No @GeneratedValue because product_id values already exist (306 products imported)

---

## Task 2.3: Create PriceId.java class ✅

**Description**: Embeddable class for composite key.

**Acceptance criteria**:
- [x] File created
- [x] @Embeddable present
- [x] @Data present (generates getters, setters, equals, hashCode)
- [x] Implements Serializable
- [x] Properties: productId (Integer), date (LocalDate) with @Column mapping
- [x] Compiles without errors

**Design Decision**: Used @Embeddable + @EmbeddedId instead of @IdClass (more OOP)

---

## Task 2.4: Create Price.java entity ✅

**Description**: Map `prices` table with composite key.

**Acceptance criteria**:
- [x] @Entity, @Table(name = "prices")
- [x] @EmbeddedId private PriceId id (NOT @IdClass)
- [x] Price properties using BigDecimal (NEVER double/float for money)
  - [x] priceMinimum, priceMaximum, priceAverage, priceMedian (all BigDecimal)
  - [x] priceStandardDeviation (nullable, BigDecimal)
- [x] Properties: storeCount (Integer), offerCount (Integer), offerPercentage (BigDecimal)
- [x] product property with @ManyToOne(fetch = FetchType.LAZY)
- [x] @JoinColumn(name = "product_id", insertable = false, updatable = false)
- [x] Compiles without errors

**Critical Decision**: BigDecimal for all monetary values - floating point arithmetic causes precision loss

---

## Task 2.5: Create Client.java entity ✅

**Description**: Map `clients` table.

**Acceptance criteria**:
- [x] @Entity, @Table(name = "clients")
- [x] clientId property with @Id, @GeneratedValue(strategy = GenerationType.IDENTITY) as Long
- [x] Properties: username, email, password, apiKey with @Column (all unique/not null)
- [x] Properties: isActive with @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
- [x] Timestamp properties: createdAt, updatedAt
- [x] Compiles without errors

**Notes**: Used Long for clientId (BIGSERIAL maps to long, not int)

---

## Task 2.6: Create CategoryRepository.java ✅

**Description**: Repository interface for categories.

**Acceptance criteria**:
- [x] Extends JpaRepository<Category, Integer>
- [x] @Repository present
- [x] Method findByName(String name) returns Optional<Category>
- [x] Method findAllByOrderByNameAsc() returns List<Category>
- [x] Compiles without errors

**Generated Methods** (automatic from JpaRepository):
- findById(Integer), findAll(), save(), delete(), count()

---

## Task 2.7: Create ProductRepository.java ✅

**Description**: Repository interface for products.

**Acceptance criteria**:
- [x] Extends JpaRepository<Product, Integer>
- [x] @Repository present
- [x] Method findByCategory(Category category) returns List<Product>
- [x] Method findByBrand(String brand) returns List<Product>
- [x] Method findByNameContainingIgnoreCase(String name) returns List<Product>
- [x] Method @Query searchByNameKeyword(@Param("keyword") String keyword) with LIKE
- [x] Compiles without errors

**Query Details**: searchByNameKeyword uses LOWER + CONCAT + LIKE for case-insensitive search

---

## Task 2.8: Create PriceRepository.java ✅

**Description**: Repository interface for prices (composite key).

**Acceptance criteria**:
- [x] Extends JpaRepository<Price, PriceId>
- [x] @Repository present
- [x] Method findByIdProductId(Integer productId) returns List<Price>
- [x] Method findByIdProductIdOrderByIdDateDesc(Integer productId) returns List<Price>
- [x] Method findByIdDateBetween(LocalDate start, LocalDate end) returns List<Price>
- [x] Method findByIdProductIdAndIdDateBetween(Integer, LocalDate, LocalDate) returns List<Price>
- [x] Compiles without errors

**Design Note**: Field paths use `Id.productId` and `Id.date` because PriceId is @EmbeddedId

---

## Task 2.9: Create ClientRepository.java ✅

**Description**: Repository interface for clients (authentication).

**Acceptance criteria**:
- [x] Extends JpaRepository<Client, Long>
- [x] @Repository present
- [x] Method findByUsername(String username) returns Optional<Client>
- [x] Method findByEmail(String email) returns Optional<Client>
- [x] Method findByApiKey(String apiKey) returns Optional<Client>
- [x] Compiles without errors

**Used in Phase 3** for authentication and API key validation

---

## Task 2.10: Create ProductController.java ✅

**Description**: REST controller with basic endpoints.

**Acceptance criteria**:
- [x] @RestController present
- [x] @RequestMapping("/api/v1/products")
- [x] Injects ProductService via constructor with @AllArgsConstructor
- [x] Endpoint GET /api/v1/products returns List<Product> (all 306 records)
- [x] Endpoint GET /api/v1/products/{id} returns ResponseEntity<Product>
- [x] Endpoint GET /api/v1/products/search?query=X returns List<Product>
- [x] Endpoint order: /search BEFORE /{id} (prevents routing conflict)
- [x] Compiles without errors

**Tested Responses**:
- GET /api/v1/products → HTTP 200, JSON array
- GET /api/v1/products/15 → HTTP 200, {"productId":15, "name":"...", "category":{...}}
- GET /api/v1/products/999 → HTTP 404 (not found)
- GET /api/v1/products/search?query=arroz → HTTP 200, array of 6 rice products

---

## Task 2.11: Verify integration ✅

**Description**: Start Spring Boot and test endpoints.

**Acceptance criteria**:
- [x] Spring Boot starts without errors (using -Dspring-boot.run.profiles=dev)
- [x] No Hibernate schema mismatch (Flyway V1 + V2 verified)
- [x] GET /api/v1/products returns JSON array with all 306 products
- [x] GET /api/v1/products/15 returns product JSON with category
- [x] GET /api/v1/products/search?query=arroz returns 6 rice products
- [x] Correct HTTP status codes:
  - [x] 200 OK for successful requests
  - [x] 404 Not Found for non-existent product
- [x] No circular reference errors (Category.products is @JsonIgnore)
- [x] Spring Security disabled in dev profile (SecurityConfig)

**Configuration Used**:
- Profile: dev (application-dev.yaml)
- Security: Disabled (SecurityConfig permits all requests)
- Logging: Hibernate SQL logging enabled for debugging

---

## Task 2.12: (Optional) Create tests ⏭️

**Description**: Tests for repositories and controller.

**Status**: DEFERRED to Phase 3 (optional enhancement)

**Planned**:
- [ ] ProductRepositoryTest with @DataJpaTest
  - [ ] testFindById()
  - [ ] testFindByBrand()
  - [ ] testSearchByName()
- [ ] ProductControllerTest with @WebMvcTest
  - [ ] testGetAllProducts()
  - [ ] testGetProductById()
  - [ ] testGetProductByIdNotFound()
  - [ ] testSearchByName()
- [ ] Tests run without errors
- [ ] Coverage > 70%

**Rationale**: High ROI for learning but lower priority than auth layer in Phase 3

---

## Final validation ✅

| Component | Status | Details |
|-----------|--------|---------|
| **Entities** | ✅ 5 created | Category, Product, Price, PriceId, Client |
| **Repositories** | ✅ 4 created | CategoryRepository, ProductRepository, PriceRepository, ClientRepository |
| **Services** | ✅ 3 created | ProductService, PriceService, CategoryService |
| **Controller** | ✅ 3 endpoints | GET /api/v1/products, /{id}, /search |
| **Spring Boot** | ✅ Running | Profile: dev, Security: disabled |
| **Endpoints** | ✅ Tested | All return correct JSON, proper status codes |
| **Database** | ✅ Connected | 306 products × 134 categories, 774k prices |
| **Compilation** | ✅ No errors | Maven clean compile successful |

---

## Key Learning Points

1. **@EmbeddedId vs @IdClass**: Chose @EmbeddedId for better OOP
2. **BigDecimal for prices**: Critical for avoiding floating-point precision loss
3. **Lazy Loading**: Prevents N+1 problem, managed with @JsonIgnore
4. **Spring Data JPA**: Query methods auto-generated from names
5. **Service Layer**: Separates HTTP concerns from business logic
6. **Spring Security**: Disabled in dev via custom SecurityConfig

---

**Next phase**: 03-backend-auth-security (JWT, API keys, Client authentication)
