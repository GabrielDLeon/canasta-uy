---
status: complete
updated: 2026-02-28
---

# Phase 4: Backend - Business Logic & Analytics - PLAN

**Objective**: Complete API functionality with prices, categories, and analytics using daily data with Redis caching.

**Scope**: Final development phase - Core API complete after this.

---

## Implementation Strategy

### Part A: Prices Endpoints

**1. Product Prices Sub-resource**
- Endpoint: `GET /api/v1/products/{id}/prices`
- Query params: from, to, granularity
- Max date range: 365 days
- Returns: Price history for single product
- Cache: 5 minutes

**2. Global Prices Resource**
- Endpoint: `GET /api/v1/prices`
- Query params: product_ids, category_id, from, to, granularity
- Returns: Filtered price records (paginated)
- Use cases: Multi-product comparison, category analysis
- Cache: 5 minutes

### Part B: Categories Endpoints

**3. Category Products**
- Endpoint: `GET /api/v1/categories/{id}/products`
- Returns: Paginated list of products in category

**4. Category Stats**
- Endpoint: `GET /api/v1/categories/{id}/stats`
- Returns: Aggregated statistics (avg, min, max prices)
- Cache: 15 minutes

### Part C: Analytics Endpoints (Hybrid Format)

**5. Price Trend**
- Endpoint: `GET /api/v1/analytics/trend/{productId}`
- Query param: include_data (boolean)
- Returns: Trend direction, variation %, volatility + optional raw data
- Cache: 1 hour

**6. Category Inflation**
- Endpoint: `GET /api/v1/analytics/inflation/{categoryId}`
- Returns: Inflation rate, monthly breakdown
- Cache: 1 hour

**7. Product Comparison**
- Endpoint: `GET /api/v1/analytics/compare`
- Query param: product_ids (comma-separated)
- Returns: Side-by-side comparison, price differences

**8. Top Price Changes**
- Endpoint: `GET /api/v1/analytics/top-changes`
- Query params: period (7d, 30d, 90d, 1y), type (increase/decrease/all)
- Returns: Products with highest price variations

---

## Caching Strategy

**TTL Configuration**:
- Price queries: 5 minutes (fresher data needed)
- Category stats: 15 minutes
- Analytics: 1 hour (expensive to compute)

**Cache Keys**: Include all query parameters for uniqueness

---

## Validation Rules

**Date Ranges**:
- Maximum 365 days per request
- Return 400 Bad Request if exceeded
- Default: last 365 days if not specified

**Pagination**:
- Default page size: 20
- Maximum page size: 100

**Authentication**: All endpoints require API Key

---

## Success Criteria

- [ ] All 8 endpoints implemented and working
- [ ] Date range validation enforced (max 365 days)
- [ ] Redis cache reducing response times
- [ ] Bruno tests covering all scenarios
- [ ] API documentation complete
- [ ] Response times <200ms for cached queries

---

**Last Updated**: 2026-02-28
