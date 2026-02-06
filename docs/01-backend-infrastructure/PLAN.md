---
status: completed
updated: 2026-02-06
---

# Phase 1: Backend Infrastructure - PLAN

**Objective**: Set up complete infrastructure (Docker, Spring Boot, PostgreSQL, Redis, Flyway) and import 774,716 price records.

---

## Implementation Plan

### 1. Docker infrastructure setup

Create `docker-compose.yml` that starts three services:
- **PostgreSQL 15**: Relational database for persistent data
- **Redis 7**: High-performance cache
- **pgAdmin**: Web tool for managing PostgreSQL

Each service will have healthchecks, volumes for persistence, and internal networks configured. Services start in order: PostgreSQL first, then dependents.

### 2. Spring Boot Configuration

Create Spring Boot 4.0.2 project with Java 21 and Maven. Configure two profiles:
- **application.yaml**: Base configuration (datasource, JPA, Flyway, logging)
- **application-dev.yaml**: Development profile that temporarily excludes Security and Redis

The idea is to have dependencies ready in `pom.xml` for Phase 3 (Auth), but not activated in development.

### 3. Create Flyway migrations

**V1__create_schema.sql**: Defines database schema
- `categories` table: 134 product categories
- `products` table: 306 products with FK to categories
- `prices` table: 774,716 price records with composite key (product_id, date)
- `clients` table: For authentication (Phase 3, empty for now)

**V2__create_indexes.sql**: Query optimization
- Indexes in `prices` for searches by date, product_id, and composite
- Indexes in `products` for searches by category and brand

### 4. Prepare data for import

Create Python script `prepare_db_import.py` that:
- Reads product CSV (306 products with categories as strings)
- Extracts unique categories (134) and generates `categories.csv`
- Maps each product to its `category_id` and generates `products_db.csv`
- Filters price CSV to necessary columns  `prices_db.csv`
- Validates referential integrity before export

Output: 3 CSVs ready to import to PostgreSQL.

### 5. Import data with \COPY

Execute three COPY commands in order:
1. Import `categories.csv` (134 records)
2. Import `products_db.csv` (306 records, with FK to categories)
3. Import `prices_db.csv` (774,716 records, with FK to products)

This respects foreign keys and CHECK constraints.

### 6. Validate everything works

- Start Spring Boot with dev profile
- Flyway executes V1 and V2 automatically
- Hibernate validates that JPA entities match the schema
- Run test queries to verify referential integrity
- Confirm final counts

---

## Architectural Decisions

### Spring Boot profiles for development
Use `application-dev.yaml` to temporarily exclude Spring Security and Redis. Dependencies remain in `pom.xml` ready for Phase 3 (Auth).

### Normalized schema
Separate `categories` table with FK from `products`. This improves referential integrity and enables 1:N relationships. Alternative would be to have `category` as VARCHAR in products.

### Composite key in prices
The PK of prices is `(product_id, date)`. This reflects the nature of the data: one unique record per product per day.

### CSV transformation with script
Instead of manually manipulating CSVs directly, use automated Python script. This is reproducible and validates referential integrity before import.

### Import method: \COPY
Choose `\COPY` (psql command) for its simplicity and speed. Evaluated alternatives: Spring Batch (more robust for complex processes), pgloader (faster for very large datasets), Flyway V3 migration (more integrated but requires Docker volumes).

---

## File structure

```
backend/
── docker-compose.yml
── pom.xml
── src/main/java/uy/eleven/canasta/
   ── CanastaApplication.java
── src/main/resources/
    ── application.yaml
    ── application-dev.yaml
    ── db/migration/
        ── V1__create_schema.sql
        ── V2__create_indexes.sql

scripts/
── prepare_db_import.py

data/processed/
── db_import/
    ── categories.csv
    ── products_db.csv
    ── prices_db.csv
```

---

## Final data

- **categories**: 134 records
- **products**: 306 records
- **prices**: 774,716 records
- **Temporal coverage**: 10 years of historical data

---

## Known issues

**Spring Security configuration**: Spring Security activates by default even though we try to exclude it temporarily in development. It will be configured correctly in Phase 3.

**Open-In-View warning**: Hibernate keeps session open during render. Can cause lazy-loading issues. Will be disabled in production.

---

## References

- `CONTEXT.md` - Current status and decisions
- `TASKS.md` - Task checklist
- `database-schema.md` - Complete PostgreSQL schema
