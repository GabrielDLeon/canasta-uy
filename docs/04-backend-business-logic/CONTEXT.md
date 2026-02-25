---
status: in_progress
updated: 2026-02-25
---

# Phase 4: Backend - Business Logic & Analytics - CONTEXT

**Status**: In Progress  
**Objective**: Complete API with prices, categories, and analytics endpoints with Redis caching

---

## Key Concepts

### 1. Hybrid Analytics Response Format

Different consumers need different data formats:
- **Dashboards/Reports** need processed insights (summary)
- **Charts/Visualizations** need raw data points (series)

**Solution**: Single endpoint returns both:
- `summary` field always present (processed data)
- `data` field optional via query parameter (raw series for graphing)

### 2. Daily Data + Redis Cache Strategy

**Decision**: Keep daily granularity, cache expensive queries

**Why not monthly tables?**
- 774k records is manageable for PostgreSQL with proper indexes
- Daily data allows flexible aggregation (weekly, monthly, quarterly on-demand)
- Simpler data model, no sync complexity
- Cache solves performance for frequent queries

**Cache Strategy**:
- Analytics endpoints: 1 hour TTL
- Category stats: 15 minutes TTL  
- Price history: 5 minutes TTL

### 3. Prices Resource Design

**Two complementary approaches**:

**Sub-resource**: `/products/{id}/prices`
- Use case: "Show me this product's price history"
- More intuitive UX for single product view

**Independent resource**: `/prices`
- Use case: "Find prices matching these criteria"
- Advanced filters: multiple products, categories, date ranges

### 4. One Year Limit

**Why?** Dataset has 10 years (2016-2025) = 3,650+ days per product
- Unlimited queries could be very slow
- Most use cases need 1 year or less
- Forces clients to paginate/batch requests

**Rule**: Maximum 365 days per request

### 5. Granularity

**Daily** (default):
- Complete precision
- 365 points max per product/year

**Monthly** (optional):
- Pre-aggregated for dashboards
- 12 points per product/year
- Faster for long-term trends

---

## Architectural Decisions

### Decision 1: Cache-Aside Pattern
Simple, flexible, no cache infrastructure complexity. Check Redis first, query DB on miss.

### Decision 2: No Separate Monthly Table
Adds complexity for marginal gain at current scale. Reconsider if >10M records.

### Decision 3: Processed + Raw Data in Same Response
Single endpoint covers both use cases via optional flag.

---

## Endpoints Overview

### Products & Prices
- `GET /api/v1/products/{id}/prices` - Product price history
- `GET /api/v1/prices` - Global price search with filters

### Categories
- `GET /api/v1/categories/{id}/products` - Products in category (paginated)
- `GET /api/v1/categories/{id}/stats` - Aggregated statistics

### Analytics (Hybrid Format)
- `GET /api/v1/analytics/trend/{productId}` - Price trend analysis
- `GET /api/v1/analytics/inflation/{categoryId}` - Inflation by category
- `GET /api/v1/analytics/compare` - Compare multiple products
- `GET /api/v1/analytics/top-changes` - Top price variations

---

## After This Phase

**Phase 5** (optional): Polish & Production
- SSL/HTTPS setup
- Comprehensive testing
- Monitoring/logging

**No more core development** after Phase 4.

---

**Last Updated**: 2026-02-25
