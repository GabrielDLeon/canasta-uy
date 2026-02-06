---
status: completed
updated: 2026-02-06
---

# Phase 1: Backend Infrastructure - TASKS

---

## Task 1.1: Create docker-compose.yml

**Description**: Configure service orchestration (PostgreSQL, Redis, pgAdmin).

**Acceptance criteria**:
- [x] File `docker-compose.yml` created
- [x] PostgreSQL 15 configured on port 5432
- [x] Redis 7 configured on port 6379
- [x] pgAdmin configured on port 5050
- [x] Healthchecks configured for all services
- [x] Volumes for data persistence
- [x] Internal networks configured
- [x] `docker compose up -d` starts all services without errors
- [x] `docker ps` shows 3 containers with status "Up"

---

## Task 1.2: Configure Spring Boot

**Description**: Create Spring Boot 4.0.2 project with Java 21.

**Acceptance criteria**:
- [x] Spring Boot 4.0.2 project with Java 21 created
- [x] `pom.xml` with necessary dependencies
- [x] `CanastaApplication.java` class with `@SpringBootApplication`
- [x] Compiles without errors: `./mvnw clean compile`

---

## Task 1.3: Create application.yaml

**Description**: Base Spring Boot configuration.

**Acceptance criteria**:
- [x] File `application.yaml` created
- [x] PostgreSQL datasource configured
- [x] JPA configured (ddl-auto: validate)
- [x] Flyway enabled
- [x] Logging configured

---

## Task 1.4: Create application-dev.yaml

**Description**: Development profile without Security and Redis.

**Acceptance criteria**:
- [x] File `application-dev.yaml` created
- [x] `spring.autoconfigure.exclude` disables unnecessary components in dev
- [x] Logs configured to DEBUG

---

## Task 1.5: Create Flyway migration V1

**Description**: Define database schema.

**Acceptance criteria**:
- [x] File `V1__create_schema.sql` created
- [x] `categories` table defined (134 records expected)
- [x] `products` table defined with FK to categories (306 records expected)
- [x] `prices` table defined with composite PK (774,716 records expected)
- [x] `clients` table defined for Phase 3
- [x] CHECK constraints configured

---

## Task 1.6: Create Flyway migration V2

**Description**: Define indexes to optimize queries.

**Acceptance criteria**:
- [x] File `V2__create_indexes.sql` created
- [x] Indexes in `prices` (date, product_id, composite)
- [x] Indexes in `products` (category_id, brand)

---

## Task 1.7: Create script prepare_db_import.py

**Description**: Transform CSVs for PostgreSQL import.

**Acceptance criteria**:
- [x] Script `prepare_db_import.py` created
- [x] Reads product CSV
- [x] Extracts 134 unique categories  `categories.csv`
- [x] Maps category string  category_id  `products_db.csv`
- [x] Filters price columns  `prices_db.csv`
- [x] Validates referential integrity
- [x] Executes without errors

---

## Task 1.8: Import data to PostgreSQL

**Description**: Import the 3 generated CSVs.

**Acceptance criteria**:
- [x] `categories.csv` imported: 134 records
- [x] `products_db.csv` imported: 306 records
- [x] `prices_db.csv` imported: 774,716 records
- [x] No FK or constraint errors

---

## Task 1.9: Validate referential integrity

**Description**: Execute queries to verify data.

**Acceptance criteria**:
- [x] Query with JOINs works
- [x] All FKs are valid
- [x] CHECK constraints validated
- [x] Complete temporal range

---

## Task 1.10: Verify Spring Boot starts

**Description**: Start Spring Boot and confirm Flyway executes.

**Acceptance criteria**:
- [x] Spring Boot starts without errors
- [x] Flyway executes V1 and V2 automatically
- [x] Hibernate validates schema without mismatches
- [x] Spring Boot listens on port 8080

---

## Final Results

 Component  Status 
-------------------
 Docker Compose  OK 
 Spring Boot  OK 
 PostgreSQL  OK 
 Flyway  OK 
 Data  OK 

---

**Next phase**: 02-backend-domain-persistence
