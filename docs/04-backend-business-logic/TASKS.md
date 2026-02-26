---
status: in_progress
updated: 2026-02-26
---

# Phase 4: Backend - Business Logic & Analytics - TASKS

This phase completes the CanastaUY API with prices, categories, and analytics functionality.

---

## Quick Links

- **Architecture & Concepts**: See [CONTEXT.md](./CONTEXT.md)
- **Implementation Plan**: See [PLAN.md](./PLAN.md)
- **Current Status**: See [STATUS.md](./STATUS.md)

---

## Prerequisites

Before starting this phase, ensure:
- [x] Phase 3 complete (authentication working)
- [x] Database migrated (V1, V2, V3 applied)
- [x] Redis running and connected
- [x] Data imported (774k price records)

---

## Part A: Infrastructure

### Task 4.1: Create CacheConfig

**Description**: Configure Spring Cache with Redis.

**Acceptance criteria**:
- [x] Config class created in `config/` package
- [x] `@EnableCaching` annotation
- [x] Redis cache manager configured
- [x] TTL settings: 5min (prices), 15min (categories), 1h (analytics)
- [x] Cache names defined: `prices`, `categories`, `analytics`

**Files**:
- `backend/src/main/java/uy/eleven/canasta/config/CacheConfig.java`

---

### Task 4.2: Update application.yaml with cache settings

**Description**: Add cache configuration to YAML files.

**To add**:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour default

canasta:
  cache:
    prices:
      ttl: 300000        # 5 minutes
    categories:
      ttl: 900000        # 15 minutes
    analytics:
      ttl: 3600000       # 1 hour
```

**Acceptance criteria**:
- [x] Cache settings added to `application.yaml`
- [x] YAML syntax valid

---

## Part B: DTOs

### Task 4.3: Create Price DTOs

**Description**: DTOs for price endpoints.

**DTOs to create**:

**price/PriceResponse.java**:
```java
record PriceResponse(
    LocalDate date,
    BigDecimal priceMin,
    BigDecimal priceMax,
    BigDecimal priceAvg,
    BigDecimal priceMedian,
    Integer storeCount,
    BigDecimal offerPercentage
) {}
```

**price/PriceListResponse.java**:
```java
record PriceListResponse(
    Integer productId,
    String productName,
    DateRange period,
    String granularity,
    List<PriceResponse> prices
) {}
```

**price/PriceSearchResponse.java**:
```java
record PriceSearchResponse(
    List<PriceResponse> prices,
    PaginationInfo pagination
) {}
```

**Acceptance criteria**:
- [x] All DTOs created in `dto/price/` package
- [x] Use Java records
- [x] Include validation annotations where needed
- [x] Serializable for caching

---

### Task 4.4: Create Category DTOs

**Description**: DTOs for category endpoints.

**DTOs to create**:

**category/CategoryProductsResponse.java**:
```java
record CategoryProductsResponse(
    Integer categoryId,
    String categoryName,
    List<ProductSummary> products,
    PaginationInfo pagination
) {}
```

**category/CategoryStatsResponse.java**:
```java
record CategoryStatsResponse(
    Integer categoryId,
    String categoryName,
    Integer productCount,
    DateRange period,
    PriceStats stats
) {}
```

**Acceptance criteria**:
- [x] DTOs created in `dto/category/` package
- [x] Use Java records
- [x] Include nested records for complex data

---

### Task 4.5: Create Analytics DTOs

**Description**: DTOs for analytics endpoints (hybrid format).

**DTOs to create**:

**analytics/TrendResponse.java**:
```java
record TrendResponse(
    Integer productId,
    String productName,
    DateRange period,
    TrendSummary summary,
    List<PricePoint> data  // null if include_data=false
) {}

record TrendSummary(
    String trend,                    // "increasing", "decreasing", "stable"
    String trendDirection,           // "up", "down", "flat"
    BigDecimal variationPercentage,
    BigDecimal variationAbsolute,
    BigDecimal priceStart,
    BigDecimal priceEnd,
    BigDecimal priceAvg,
    BigDecimal priceMin,
    BigDecimal priceMax,
    String volatility               // "low", "medium", "high"
) {}

record PricePoint(
    LocalDate date,
    BigDecimal priceAvg,
    BigDecimal priceMin,
    BigDecimal priceMax
) {}
```

**analytics/InflationResponse.java**:
```java
record InflationResponse(
    Integer categoryId,
    String categoryName,
    DateRange period,
    InflationSummary summary,
    List<MonthlyInflation> data
) {}
```

**analytics/ComparisonResponse.java**:
```java
record ComparisonResponse(
    DateRange period,
    List<ProductComparison> products,
    ComparisonStats comparison
) {}
```

**analytics/TopChangesResponse.java**:
```java
record TopChangesResponse(
    String period,
    DateRange dateRange,
    List<PriceChange> changes
) {}
```

**Acceptance criteria**:
- [x] All DTOs created in `dto/analytics/` package
- [x] Use Java records
- [x] Nested records for complex structures
- [x] Data field is optional/nullable

---

### Task 4.6: Create Common DTOs

**Description**: Shared DTOs.

**DTOs to create**:

**common/DateRange.java**:
```java
record DateRange(LocalDate from, LocalDate to) {}
```

**common/PaginationInfo.java**:
```java
record PaginationInfo(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}
```

**Acceptance criteria**:
- [x] DTOs created in `dto/common/` package
- [x] Reusable across endpoints

---

## Part C: Services

### Task 4.7: Update PriceService

**Description**: Enhance PriceService with new methods.

**Methods to add**:
```java
List<Price> getPricesByProductAndDateRange(Integer productId, LocalDate from, LocalDate to);
List<Price> getPricesByProductAndDateRange(Integer productId, LocalDate from, LocalDate to, Granularity granularity);
Page<Price> searchPrices(List<Integer> productIds, Integer categoryId, LocalDate from, LocalDate to, Pageable pageable);
```

**Acceptance criteria**:
- [x] Methods implemented in `PriceService.java`
- [x] DTO mapping methods added
- [x] Use existing repository methods

---

### Task 4.8: Update CategoryService

**Description**: Add methods for category stats.

**Methods to add**:
```java
Page<Product> getProductsByCategory(Integer categoryId, Pageable pageable);
CategoryStats calculateCategoryStats(Integer categoryId, LocalDate from, LocalDate to);
```

**Acceptance criteria**:
- [x] Methods implemented in `CategoryService.java`
- [x] Pagination support
- [x] Stats calculation implemented

---

### Task 4.9: Create AnalyticsService

**Description**: Business logic for analytics calculations.

**Methods to implement**:
```java
TrendResponse calculateTrend(Integer productId, LocalDate from, LocalDate to, boolean includeData);
InflationResponse calculateInflation(Integer categoryId, LocalDate from, LocalDate to, boolean includeData);
ComparisonResponse compareProducts(List<Integer> productIds, LocalDate from, LocalDate to);
TopChangesResponse getTopChanges(String period, String type, Integer limit, Integer categoryId);
```

**Calculation logic**:
- **Trend**: Compare first and last price, calculate % change
- **Volatility**: Coefficient of variation (std/avg)
- **Inflation**: Weighted average across category products
- **Top changes**: Sort by absolute % change

**Acceptance criteria**:
- [x] Service created in `service/` package - Structure with method signatures
- [ ] All calculation methods implemented - **USER IMPLEMENTATION REQUIRED**
- [ ] Proper handling of edge cases (missing data, single point)
- [ ] Unit-tested calculations

---

### Task 4.10: Add caching annotations

**Description**: Add @Cacheable to expensive operations.

**Methods to cache**:
- `PriceService.getPricesByProductAndDateRange()` - 5 min
- `CategoryService.calculateCategoryStats()` - 15 min
- `AnalyticsService.calculateTrend()` - 1 hour
- `AnalyticsService.calculateInflation()` - 1 hour

**Acceptance criteria**:
- [ ] `@Cacheable` annotations added
- [ ] Cache keys are unique and deterministic
- [ ] Cache names match CacheConfig

---

## Part D: Controllers

### Task 4.11: Create PriceController

**Description**: REST endpoints for prices.

**Endpoints to implement**:

**GET /api/v1/products/{id}/prices**
```java
@GetMapping("/products/{id}/prices")
public ResponseEntity<PriceListResponse> getProductPrices(
    @PathVariable Integer id,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
    @RequestParam(defaultValue = "daily") String granularity
)
```

**GET /api/v1/prices**
```java
@GetMapping("/prices")
public ResponseEntity<PriceSearchResponse> searchPrices(
    @RequestParam(required = false) String productIds,
    @RequestParam(required = false) Integer categoryId,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
    @RequestParam(defaultValue = "daily") String granularity,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
)
```

**Acceptance criteria**:
- [x] Controller created in `controller/` package
- [x] All endpoints implemented
- [x] Date range validation via Request DTOs
- [x] Proper HTTP status codes (200, 400, 404)
- [x] Input validation with @Valid
- [ ] API Key authentication required

---

### Task 4.12: Update CategoryController

**Description**: Add new endpoints to existing controller.

**Endpoints to add**:

**GET /api/v1/categories/{id}/products**
```java
@GetMapping("/{id}/products")
public ResponseEntity<CategoryProductsResponse> getCategoryProducts(
    @PathVariable Integer id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
)
```

**GET /api/v1/categories/{id}/stats**
```java
@GetMapping("/{id}/stats")
public ResponseEntity<CategoryStatsResponse> getCategoryStats(
    @PathVariable Integer id,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to
)
```

**Acceptance criteria**:
- [x] Endpoints added to `CategoryController.java`
- [x] Pagination working
- [x] Date range validation via Request DTOs
- [ ] API Key authentication required

---

### Task 4.13: Create AnalyticsController

**Description**: REST endpoints for analytics.

**Endpoints to implement**:

**GET /api/v1/analytics/trend/{productId}**
```java
@GetMapping("/trend/{productId}")
public ResponseEntity<TrendResponse> getTrend(
    @PathVariable Integer productId,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
    @RequestParam(defaultValue = "false") boolean includeData
)
```

**GET /api/v1/analytics/inflation/{categoryId}**
```java
@GetMapping("/inflation/{categoryId}")
public ResponseEntity<InflationResponse> getInflation(
    @PathVariable Integer categoryId,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
    @RequestParam(defaultValue = "false") boolean includeData
)
```

**GET /api/v1/analytics/compare**
```java
@GetMapping("/compare")
public ResponseEntity<ComparisonResponse> compareProducts(
    @RequestParam String productIds,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to
)
```

**GET /api/v1/analytics/top-changes**
```java
@GetMapping("/top-changes")
public ResponseEntity<TopChangesResponse> getTopChanges(
    @RequestParam(defaultValue = "30d") String period,
    @RequestParam(defaultValue = "all") String type,
    @RequestParam(defaultValue = "10") int limit,
    @RequestParam(required = false) Integer categoryId
)
```

**Acceptance criteria**:
- [x] Controller created in `controller/` package - Structure with method signatures
- [ ] All 4 endpoints implemented - **USER IMPLEMENTATION REQUIRED**
- [ ] Date range validation
- [ ] Product ID list parsing (comma-separated)
- [ ] API Key authentication required

---

## Part E: Repositories

### Task 4.14: Update PriceRepository

**Description**: Add custom queries for analytics.

**Methods to add**:
```java
@Query("SELECT p FROM Price p WHERE p.id.productId IN :productIds AND p.id.date BETWEEN :from AND :to")
List<Price> findByProductIdsAndDateRange(@Param("productIds") List<Integer> productIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

@Query("SELECT AVG(p.priceAverage), MIN(p.priceAverage), MAX(p.priceAverage) FROM Price p WHERE p.id.productId = :productId AND p.id.date BETWEEN :from AND :to")
PriceAggregation aggregateByProductAndDateRange(@Param("productId") Integer productId, @Param("from") LocalDate from, @Param("to") LocalDate to);
```

**Acceptance criteria**:
- [x] Custom queries added to `PriceRepository.java`
- [x] DTO projections (PriceAggregation, PriceChange, DailyPriceAverage)
- [x] Use @Query for complex aggregations

---

## Part F: Validation

### Task 4.15: Create DateRangeValidator

**Description**: Custom validator for date ranges.

**Implementation**:
```java
@Component
public class DateRangeValidator {
    private static final int MAX_DAYS = 365;
    
    public void validate(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("'from' date must be before 'to' date");
            }
            if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
                throw new IllegalArgumentException("Date range cannot exceed 365 days");
            }
        }
    }
}
```

**Acceptance criteria**:
- [x] Validation implemented via Request DTOs with @Valid
- [x] Date range validation in DateRangeRequest constructor
- [x] Returns clear error messages

---

### Task 4.16: Update GlobalExceptionHandler

**Description**: Add handlers for new exceptions.

**Exceptions to handle**:
- `IllegalArgumentException` (date validation) → 400 Bad Request
- `EmptyResultDataAccessException` → 404 Not Found
- Calculation errors → 500 Internal Server Error

**Acceptance criteria**:
- [x] GlobalExceptionHandler exists with base handlers
- [ ] Additional handlers needed for new exceptions (ValidationException, etc.)

---

## Part G: Testing

### Task 4.17: Create Bruno tests - Prices endpoints

**Description**: Bruno tests for price endpoints.

**Test files**:
- `bruno-collection/prices/01-product-prices.bru`
- `bruno-collection/prices/02-search-prices.bru`
- `bruno-collection/prices/03-date-validation.bru`

**Test scenarios**:
- Get prices for valid product
- Search prices with filters
- Test date range validation (>365 days should fail)
- Test invalid product ID (404)
- Test pagination

**Acceptance criteria**:
- [ ] Test files created
- [ ] All scenarios covered
- [ ] Tests use API key authentication
- [ ] Tests pass

---

### Task 4.18: Create Bruno tests - Categories endpoints

**Description**: Bruno tests for category endpoints.

**Test files**:
- `bruno-collection/categories/01-category-products.bru`
- `bruno-collection/categories/02-category-stats.bru`

**Test scenarios**:
- Get products in category with pagination
- Get category stats
- Test invalid category ID

**Acceptance criteria**:
- [ ] Test files created
- [ ] Tests pass

---

### Task 4.19: Create Bruno tests - Analytics endpoints

**Description**: Bruno tests for analytics endpoints.

**Test files**:
- `bruno-collection/analytics/01-trend.bru`
- `bruno-collection/analytics/02-inflation.bru`
- `bruno-collection/analytics/03-compare.bru`
- `bruno-collection/analytics/04-top-changes.bru`

**Test scenarios**:
- Get trend with and without data
- Verify calculations are correct
- Test product comparison
- Test top changes filtering

**Acceptance criteria**:
- [ ] Test files created
- [ ] All endpoints tested
- [ ] Calculation accuracy verified
- [ ] Tests pass

---

### Task 4.20: Test cache behavior

**Description**: Verify Redis caching works.

**Test scenario**:
1. Query analytics endpoint
2. Verify response time (slow, first request)
3. Query same endpoint again
4. Verify response time (fast, cache hit)
5. Check Redis has cached key

**Acceptance criteria**:
- [ ] Cache hit faster than cache miss
- [ ] Keys appear in Redis
- [ ] TTL respected (keys expire)

---

## Part H: Documentation

### Task 4.21: Update ENDPOINTS.md

**Description**: Document all new endpoints.

**Content to add**:
- All 8 endpoints with full specs
- Query parameters
- Request/response examples
- Error scenarios

**Acceptance criteria**:
- [ ] `docs/ENDPOINTS.md` updated
- [ ] All endpoints documented
- [ ] Examples included

---

### Task 4.22: Update OpenAPI annotations

**Description**: Add Swagger annotations to controllers.

**Annotations to add**:
- `@Operation` for endpoint description
- `@ApiResponse` for status codes
- `@Parameter` for query params

**Acceptance criteria**:
- [ ] All controllers annotated
- [ ] Swagger UI shows descriptions
- [ ] Example values included

---

## Final Verification Checklist

| Component | Status | Details |
|-----------|--------|---------|
| **Cache Config** | ⬜ Pending | CacheConfig, TTL settings |
| **DTOs** | ⬜ Pending | Price, Category, Analytics DTOs |
| **Services** | ⬜ Pending | Enhanced services with caching |
| **Controllers** | ⬜ Pending | 8 endpoints total |
| **Repositories** | ⬜ Pending | Custom queries |
| **Validation** | ⬜ Pending | Date range validator |
| **Bruno Tests** | ⬜ Pending | All endpoints tested |
| **Documentation** | ⬜ Pending | ENDPOINTS.md, OpenAPI |
| **Performance** | ⬜ Pending | <200ms response times |

---

## Key Learning Points

### Caching Strategy
1. **TTL selection**: Balance between freshness and performance
2. **Cache keys**: Must be deterministic and unique
3. **Cache invalidation**: Not needed for this read-only API

### Analytics Calculations
4. **Trend detection**: Compare first vs last price
5. **Volatility**: Coefficient of variation (std/avg)
6. **Inflation**: Weighted average across products

### API Design
7. **Hybrid format**: One endpoint serves multiple use cases
8. **Date limits**: Prevent expensive unbounded queries
9. **Pagination**: Essential for large datasets

### Performance
10. **Daily data is fine**: 774k rows is manageable with indexes
11. **Cache expensive queries**: Analytics, not simple lookups
12. **Aggregation in SQL**: Let database do the work

---

**Next**: After completion, consider Phase 5 (Polish & Production) or declare project complete.

---

**Last Updated**: 2026-02-25
