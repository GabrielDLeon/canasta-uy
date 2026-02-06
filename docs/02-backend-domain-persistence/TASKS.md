---
status: in-progress
updated: 2026-02-06
---

# Phase 2: Backend - Domain & Persistence - TASKS

---

## Task 2.1: Create Category.java entity

**Description**: Map `categories` table to JPA entity.

**Acceptance criteria**:
- [ ] File created
- [ ] @Entity, @Table(name = "categories") present
- [ ] @Data, @NoArgsConstructor, @AllArgsConstructor present
- [ ] categoryId property with @Id, @GeneratedValue
- [ ] name property with correct @Column
- [ ] createdAt property with @CreationTimestamp
- [ ] Compiles without errors

---

## Task 2.2: Update Product.java entity

**Description**: Map `products` table.

**Acceptance criteria**:
- [ ] Annotations @Entity, @Table(name = "products")
- [ ] productId property with @Id (no @GeneratedValue)
- [ ] Properties: name, specification, brand with correct @Column
- [ ] category property with @ManyToOne(fetch = FetchType.LAZY)
- [ ] Timestamp properties with @CreationTimestamp, @UpdateTimestamp
- [ ] Compiles without errors

---

## Task 2.3: Create PriceId.java class

**Description**: Embeddable class for composite key.

**Acceptance criteria**:
- [ ] File created
- [ ] @Embeddable present
- [ ] @Data, @NoArgsConstructor, @AllArgsConstructor present
- [ ] Implements Serializable
- [ ] Properties: productId, date with mapped @Column
- [ ] Compiles without errors

---

## Task 2.4: Create Price.java entity

**Description**: Map `prices` table with composite key.

**Acceptance criteria**:
- [ ] @Entity, @Table(name = "prices"), @IdClass(PriceId.class)
- [ ] Properties productId, date with @Id
- [ ] Price properties using BigDecimal (NEVER double/float)
- [ ] Properties: store_count, offer_count, offer_percentage
- [ ] product property with @ManyToOne(fetch = FetchType.LAZY)
- [ ] @JoinColumn with insertable=false, updatable=false
- [ ] Compiles without errors

---

## Task 2.5: Create Client.java entity

**Description**: Map `clients` table.

**Acceptance criteria**:
- [ ] @Entity, @Table(name = "clients")
- [ ] clientId property with @Id, @GeneratedValue
- [ ] Properties: username, email, password, apiKey with correct @Column
- [ ] Properties: isActive with default value true
- [ ] Timestamp properties
- [ ] Compiles without errors

---

## Task 2.6: Create CategoryRepository.java

**Description**: Repository interface for categories.

**Acceptance criteria**:
- [ ] Extends JpaRepository<Category, Integer>
- [ ] @Repository present
- [ ] Method findByName(String name)
- [ ] Method findAllByOrderByNameAsc()
- [ ] Compiles without errors

---

## Task 2.7: Create ProductRepository.java

**Description**: Repository interface for products.

**Acceptance criteria**:
- [ ] Extends JpaRepository<Product, Integer>
- [ ] @Repository present
- [ ] Method findByCategory(Category category)
- [ ] Method findByBrand(String brand)
- [ ] Method findByNameContainingIgnoreCase(String name)
- [ ] Method @Query searchByName(@Param("keyword") String keyword)
- [ ] Compiles without errors

---

## Task 2.8: Create PriceRepository.java

**Description**: Repository interface for prices.

**Acceptance criteria**:
- [ ] Extends JpaRepository<Price, PriceId>
- [ ] @Repository present
- [ ] Method findByProductId(Integer productId)
- [ ] Method findByProductIdOrderByDateDesc(Integer productId)
- [ ] Method findByDateBetween(LocalDate start, LocalDate end)
- [ ] Method findByProductIdAndDateBetween(...)
- [ ] Compiles without errors

---

## Task 2.9: Create ClientRepository.java

**Description**: Repository interface for clients.

**Acceptance criteria**:
- [ ] Extends JpaRepository<Client, Long>
- [ ] @Repository present
- [ ] Method findByUsername(String username) with Optional
- [ ] Method findByEmail(String email) with Optional
- [ ] Method findByApiKey(String apiKey) with Optional
- [ ] Compiles without errors

---

## Task 2.10: Create ProductController.java

**Description**: REST controller with basic endpoints.

**Acceptance criteria**:
- [ ] @RestController present
- [ ] @RequestMapping("/api/v1/products")
- [ ] Injects ProductRepository in constructor
- [ ] Endpoint GET /api/v1/products returns List<Product>
- [ ] Endpoint GET /api/v1/products/{id} returns ResponseEntity<Product>
- [ ] Endpoint GET /api/v1/products/search?query=X returns List<Product>
- [ ] Compiles without errors

---

## Task 2.11: Verify integration

**Description**: Start Spring Boot and test endpoints.

**Acceptance criteria**:
- [ ] Spring Boot starts without errors
- [ ] No Hibernate schema mismatch
- [ ] GET /api/v1/products returns JSON array with products
- [ ] GET /api/v1/products/ID returns product JSON
- [ ] GET /api/v1/products/search?query=X returns search
- [ ] Correct HTTP status codes (200 OK, 404 Not Found)

---

## Task 2.12: (Optional) Create tests

**Description**: Tests for repositories and controller.

**Acceptance criteria**:
- [ ] ProductRepositoryTest with @DataJpaTest
  - [ ] testFindById()
  - [ ] testFindByBrand()
  - [ ] testSearchByName()
- [ ] ProductControllerTest with @WebMvcTest
  - [ ] testGetAllProducts()
  - [ ] testGetProductById()
  - [ ] testGetProductByIdNotFound()
- [ ] Tests run without errors
- [ ] Coverage > 70%

---

## Final validation

 Component  Criteria 
---------------------
 Entities  5 created 
 Repositories  4 created 
 Controller  3 endpoints 
 Spring Boot  Starts without errors 
 Endpoints  Return correct JSON 

---

**Next phase**: 03-backend-auth-security
