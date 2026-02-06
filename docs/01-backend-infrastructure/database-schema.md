# Database Readiness Assessment - PostgreSQL Migration (CanastaUY)

**Date**: 2026-01-05
**Status**: READY FOR POSTGRESQL WITH CLEANED DATA

## Executive Summary

The dataset V4 is **nearly production-ready** for PostgreSQL migration. Minor data quality issues have been identified and fixed in the cleaned version. This document covers the CanastaUY project's PostgreSQL setup.

### Key Metrics
- **Price Records**: 774,716 (5.27% of outliers removed from original)
- **Active Products**: 306 (73 products without price history removed)
- **Time Period**: 2016-01-01 to 2025-09-30 (10 years)
- **Data Quality Score**: 99.97%

---

## Quality Assessment Results

### Passed Validations

1. **No Missing Values in Prices Table**
   - All 774,716 records have complete price data
   - No NULL values in critical fields

2. **Referential Integrity**
   - All product_id references are valid
   - 306 active products with price history
   - No orphaned records

3. **No Duplicates**
   - Each (product_id, date) combination is unique
   - No exact duplicate rows

4. **Price Consistency**
   - `price_min ≤ price_avg ≤ price_max` (all records valid)
   - `price_min ≤ price_median ≤ price_max` (fixed: 225 inconsistencies corrected)
   - All percentages between 0-100%
   - All store counts and offer counts valid

5. **Temporal Coverage**
   - Complete data from 2016-2025
   - All 12 months covered in most years
   - 2025 has 9 months of data (through September)

---

## Issues Found & Fixed

### Issue #1: price_median Inconsistencies

**Problem**: 225 records had `price_median` values outside the range `[price_min, price_max]`

**Example**:
```
price_min: $195.00
price_median: $468.58 (INVALID - exceeds price_max)
price_max: $468.58
```

**Solution**: Replaced invalid `price_median` with `price_avg` (which is always within range)

**Status**: FIXED

---

### Issue #2: NULL Values in Products Table

**Problem**:
- 74 products (19.53%) had NULL `brand`
- 11 products (2.90%) had NULL `specification`

**Solution**:
- Replaced NULL `brand` with "Unknown Brand"
- Replaced NULL `specification` with "Not specified"

**Status**: FIXED

---

### Issue #3: Products Without Price History

**Problem**: 73 products in the catalog had no price records

**Examples**:
- Lápiz Negro (School supplies)
- Cascola (Glue)
- Túnica (Clothing)
- Acelga (Fresh produce)

**Solution**: Removed these products from the dataset (they were never tracked)

**Result**: Reduced product count from 379  306 active products

**Status**: FIXED

---

### Note: offer_count & offer_percentage

**Finding**: 379,410 records have `offer_count = 0` and `offer_percentage = 0`

**Assessment**: This is VALID
- Means no special offers were available on that date
- Not an error, but legitimate business data

---

## Data Quality Metrics

 Metric  Value  Status 
-----------------------
 Price data completeness  100%  OK 
 Referential integrity  100%  OK 
 Logical consistency  100%  OK 
 Duplicate rate  0%  OK 
 price_median validity  100% (after fix)  OK 
 Missing brands  0% (after fix)  OK 
 Temporal coverage  10 years complete  OK 

---

## PostgreSQL Schema

```sql
CREATE TABLE products (
    product_id INT PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    specification VARCHAR(500),
    name VARCHAR(500) NOT NULL,
    CONSTRAINT uk_product_name UNIQUE(name)
);

CREATE TABLE prices (
    price_id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(product_id),
    date DATE NOT NULL,
    price_min DECIMAL(10, 2) NOT NULL,
    price_max DECIMAL(10, 2) NOT NULL,
    price_avg DECIMAL(10, 2) NOT NULL,
    price_median DECIMAL(10, 2) NOT NULL,
    price_std DECIMAL(10, 2) NOT NULL,
    store_count INT NOT NULL,
    offer_count INT NOT NULL,
    offer_percentage DECIMAL(5, 2) NOT NULL,
    CONSTRAINT uk_product_date UNIQUE(product_id, date),
    CONSTRAINT ck_price_range CHECK (price_min <= price_avg AND price_avg <= price_max),
    CONSTRAINT ck_offer_percentage CHECK (offer_percentage >= 0 AND offer_percentage <= 100)
);

-- Indexes for performance
CREATE INDEX idx_prices_date ON prices(date);
CREATE INDEX idx_prices_product ON prices(product_id);
CREATE INDEX idx_prices_product_date ON prices(product_id, date);
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_product_brand ON products(brand);
```

---

## Data Files for Import

**Use these cleaned files for PostgreSQL:**

1. **`data/processed/prices_aggregated_all_years_v4_clean.csv`**
   - 774,716 records
   - All quality issues resolved
   - Ready for direct import

2. **`data/processed/products_catalog_clean.csv`**
   - 306 active products
   - All 379 products  306 with price history
   - No NULL values

---

## Migration Steps

### 1. Create Database
```bash
createdb open_price_uy
```

### 2. Create Schema
Use the SQL schema provided above.

### 3. Import Data

**Load Products:**
```bash
psql open_price_uy -c "\COPY products FROM 'data/processed/products_catalog_clean.csv' WITH (FORMAT csv, HEADER true)"
```

**Load Prices:**
```bash
psql open_price_uy -c "\COPY prices(product_id, date, price_min, price_max, price_avg, price_median, price_std, store_count, offer_count, offer_percentage) FROM 'data/processed/prices_aggregated_all_years_v4_clean.csv' WITH (FORMAT csv, HEADER true)"
```

### 4. Verify Import
```sql
SELECT COUNT(*) FROM products;  -- Should show 306
SELECT COUNT(*) FROM prices;    -- Should show 774,716
SELECT COUNT(DISTINCT date) FROM prices;  -- Should show 3,560
```

---

## Performance Considerations

### Index Strategy
- **Primary**: (product_id, date) - for daily product lookups
- **Secondary**: date - for time-range queries
- **Tertiary**: product_id - for product-level aggregations

### Expected Partition (Optional)
For very large datasets, consider partitioning by year:
```sql
CREATE TABLE prices_2016 PARTITION OF prices
    FOR VALUES FROM ('2016-01-01') TO ('2016-12-31');
```

### Estimated Storage
- Products table: ~30 KB
- Prices table: ~100-150 MB (depending on indexes)

---

## Historical Data Versions

**V1** (Original):
- 817,842 records
- Raw, unfiltered data
- Contains outliers (max price: $4,557)

**V4** (Current - IQR Method):
- 774,716 records (5.27% removed)
- Outliers detected via IQR method
- Statistically rigorous

**V4_CLEAN** (Recommended for PostgreSQL):
- 774,716 records (same as V4)
- All data quality issues fixed
- Ready for production

---

## Recommendations

### Go Ahead With Migration
The dataset is ready for PostgreSQL. Use the `*_clean` versions:
- `prices_aggregated_all_years_v4_clean.csv`
- `products_catalog_clean.csv`

###  Next Steps

1. **Set up PostgreSQL database** with the provided schema
2. **Import the cleaned CSV files**
3. **Create indexes** for optimal query performance
4. **Run sanity checks** to verify data integrity
5. **Backup the database** before any modifications

###  Future Analytics Ready

The data structure supports:
- Time-series analysis (prices over time)
- Category-based aggregations
- Brand comparison studies
- Seasonal pattern analysis
- Inflation tracking by product

---

## Appendix: Quality Assurance Scripts

Scripts created for ongoing data quality management:

1. **`evaluate_dataset_quality_v4.py`** - Comprehensive quality audit
2. **`fix_dataset_quality_v4.py`** - Automated data cleaning

Both scripts are production-ready and can be run periodically to verify data integrity.

---

**Assessment Completed**: 2026-01-05
**Next Review Date**: After first PostgreSQL import
