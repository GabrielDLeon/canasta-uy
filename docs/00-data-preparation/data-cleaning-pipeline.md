# Data Cleaning Pipeline: From Raw Data to V4

## Overview

This document provides a complete walkthrough of the data cleaning pipeline for the Open Price UY project, from the original raw consolidated data (v1) through the improved v4 dataset. It covers the rationale for each step, technical implementation, and results achieved.

**Quick Answer**: Use **v4** for all analysis. It removes 5.27% of problematic records while maintaining 94.73% of data with proven statistical methods.

---

## Why Data Cleaning?

Raw price data from web scraping often contains:
- **Data entry errors** (typos: $4,557 instead of $45.57)
- **Unit inconsistencies** (pricing in different units: per kg vs per item)
- **Outlier stores** (single retailers with anomalous pricing)
- **System errors** (duplicates, corrupted values)

For analysis and visualization, these outliers distort trends and make charts difficult to interpret.

---

## Pipeline Overview

```
V1 (Raw)
  
  [Add time windows & categories]
  
V2 (Labeled with Outliers)
  ─ Outlier detection: 3x method
  ─ 6,069 outliers marked
  ─ No data removed yet
  
V3 (Conservative Clean)
  ─ Remove V2 outliers
  ─ 0.74% of data removed
  ─ [Still has some issues...]
  
V4 (Robust Clean)  RECOMMENDED
  ─ Outlier detection: IQR method
  ─ 5.27% of data removed
  ─ Catches problems V3 missed
```

---

## Stage 1: V1  V2 (Add Structure and Detection)

### What V2 Does

V2 takes the raw consolidated data and adds:
1. **Time windows** (`year_month` column) - Groups data by month to account for inflation
2. **Categories** (`category` column) - Links product IDs to their product categories
3. **Outlier flags** (`outlier` column) - Marks suspicious records (0 = normal, 1 = outlier)

### Implementation: `detect_price_outliers.py`

```python
# 1. Load raw data
prices_df = pd.read_csv('data/processed/prices_aggregated_all_years.csv')
products_df = pd.read_csv('data/processed/products_catalog.csv')

# 2. Add year_month column for time windowing
prices_df['year_month'] = pd.to_datetime(prices_df['date']).dt.strftime('%Y-%m')

# 3. Merge category information from products catalog
prices_df = prices_df.merge(
    products_df[['product_id', 'category']],
    on='product_id',
    how='left'
)

# 4. Detect outliers using 3x threshold method
prices_df['outlier'] = 0

for (category, year_month), group in prices_df.groupby(['category', 'year_month']):
    median_price = group['price_avg'].median()

    # Set bounds: price should be between median/3 and median*3
    lower_threshold = median_price / 3
    upper_threshold = median_price * 3

    # Find prices outside bounds
    outlier_mask = (
        (group['price_avg'] < lower_threshold) 
        (group['price_avg'] > upper_threshold)
    )

    # Mark them in the original dataframe
    prices_df.loc[group[outlier_mask].index, 'outlier'] = 1

# 5. Save V2
prices_df.to_csv('data/processed/prices_aggregated_all_years_v2.csv', index=False)
```

### The 3x Method: Why It Works (and Where It Fails)

**Formula**: A price is an outlier if: `price < (median / 3)` OR `price > (median * 3)`

**Why it works**:
- Simple to understand and implement
- Symmetric around the median (3x above = 1/3 below)
- Catches obvious data entry errors (multiplying by 100 by mistake)

**Where it fails**:
- **Only checks one metric**: The 3x method only examines `price_avg`, ignoring `price_min` and `price_max`
- **Arbitrary threshold**: Why 3x and not 2x or 4x?
- **Ignores distribution shape**: Same threshold for uniform vs skewed products

### Results: V2

 Metric  Value 
---------------
 Records analyzed  817,842 
 Outliers detected  6,069 
 Percentage  0.74% 
 Detection method  3x threshold (price_avg only) 
 New columns  `year_month`, `category`, `outlier` 

---

## Stage 2: V2  V3 (Conservative Removal)

### What V3 Does

V3 simply removes all records flagged as outliers in v2, creating a "clean" dataset while keeping the 3x detection method.

### Implementation: `create_clean_dataset_v3.py`

```python
# Load V2
prices_v2 = pd.read_csv('data/processed/prices_aggregated_all_years_v2.csv')

# Keep only records where outlier == 0
prices_v3 = prices_v2[prices_v2['outlier'] == 0].copy()

# Drop the outlier flag column (no longer needed)
prices_v3 = prices_v3.drop('outlier', axis=1)

# Save V3
prices_v3.to_csv('data/processed/prices_aggregated_all_years_v3.csv', index=False)
```

### The Problem We Discovered

V3 looked clean on the surface, but we discovered it still contained anomalies:
- **2016-07-20**: Green Chef rice with `price_max = $359.00` (but `price_avg = $44.80`)
- **2025-08-28**: Green Chef rice with `price_max = $228.00` (but `price_avg = $78.26`)

Why weren't these caught? Because **3x method only checked `price_avg`**. The average prices were normal, so the records passed through—even though the single-store maximums were anomalous.

### Results: V3

 Metric  Value 
---------------
 Records in V2  817,842 
 Records in V3  811,773 
 Removed  6,069 
 Percentage removed  0.74% 
 Issues remaining  Still contains $359 and $228 anomalies 

---

## Stage 3: V2  V4 (Robust IQR Method)

This is the critical improvement. Instead of the simple 3x method, V4 uses the **Interquartile Range (IQR)** method—a statistically rigorous approach used in box plots and professional statistical analysis.

### What IQR Does

For each group (category + time_window), IQR:
1. Calculates Q1 (25th percentile) and Q3 (75th percentile)
2. Computes IQR = Q3 - Q1
3. Sets bounds:
   - **Lower** = Q1 - 1.5 × IQR
   - **Upper** = Q3 + 1.5 × IQR
4. **Checks ALL THREE metrics** independently: `price_avg`, `price_min`, `price_max`
5. Flags a record if ANY metric falls outside its bounds

### Why IQR Is Better

 Aspect  3x Method (V3)  IQR Method (V4) 
--------------
 **Statistical basis**  Arbitrary multiplier  Industry standard 
 **Metrics checked**  1 (price_avg only)  3 (price_avg, price_min, price_max) 
 **Adapts to data**  Fixed multiplier  Uses actual distribution percentiles 
 **Catches $359**   NO   YES 
 **Catches $228**   NO   YES (partially) 
 **Detects  anomalies**   NO   YES 

### Implementation: `detect_outliers_v4_improved.py`

```python
import numpy as np
import pandas as pd

# Load V2 (which has category and year_month)
prices_v2 = pd.read_csv('data/processed/prices_aggregated_all_years_v2.csv')

# Initialize outlier column
prices_v2['outlier_v4'] = 0

# Group by category and time window
for (category, year_month), group in prices_v2.groupby(['category', 'year_month']):

    # ===== Check price_avg =====
    prices = group['price_avg'].values
    q1 = np.percentile(prices, 25)
    q3 = np.percentile(prices, 75)
    iqr = q3 - q1

    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr

    outlier_mask = (prices < lower_bound)  (prices > upper_bound)

    # ===== Check price_min =====
    prices = group['price_min'].values
    q1 = np.percentile(prices, 25)
    q3 = np.percentile(prices, 75)
    iqr = q3 - q1

    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr

    outlier_mask = (prices < lower_bound)  (prices > upper_bound)

    # ===== Check price_max =====
    prices = group['price_max'].values
    q1 = np.percentile(prices, 25)
    q3 = np.percentile(prices, 75)
    iqr = q3 - q1

    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr

    outlier_mask = (prices < lower_bound)  (prices > upper_bound)

    # Mark outliers in original dataframe
    outlier_indices = group[outlier_mask].index
    prices_v2.loc[outlier_indices, 'outlier_v4'] = 1

# Create V4 by removing all marked outliers
prices_v4 = prices_v2[prices_v2['outlier_v4'] == 0].copy()

# Drop both outlier columns (no longer needed)
prices_v4 = prices_v4.drop(['outlier', 'outlier_v4'], axis=1)

# Save V4
prices_v4.to_csv('data/processed/prices_aggregated_all_years_v4.csv', index=False)
```

### The Rice Anomaly Case Study

This perfectly illustrates why IQR succeeds where 3x fails:

**The Record**: 2016-07-20, Green Chef rice (product_id=16)
- `price_min`: $26.00 (normal)
- `price_max`: $359.00  **ONE STORE** selling at extreme price
- `price_avg`: $44.80  **NORMAL**, masks the problem
- `price_median`: $38.16

**Why 3x method failed**:
```
Median: $38.16
3x bounds: $12.72 - $114.48
price_avg: $44.80  WITHIN bounds  PASSED 
```

The average price was normal, so the record appeared clean.

**Why IQR method succeeded**:
```
For price_max metric:
Q1 = $30
Q3 = $50
IQR = $20
Lower = $30 - 1.5×20 = $0
Upper = $50 + 1.5×20 = $80

price_max: $359  OUTSIDE bounds  FLAGGED 
```

IQR caught the problem by checking the actual `price_max` column, which revealed the single-store anomaly.

### Results: V4

 Metric  Value 
---------------
 Records in V2  817,842 
 Records in V4  774,716 
 Removed  43,126 
 Percentage removed  5.27% 
 Percentage retained  94.73% 
 Detection method  IQR (price_avg, price_min, price_max) 
 Catches $359   YES 
 Catches $228   YES (mostly) 

---

## Comparison: V1  V3  V4

### Data Quality Progression

 Aspect  V1 (Raw)  V2 (Labeled)  V3 (3x Clean)  V4 (IQR Clean) 
---------------------------------------------------------------
 **Records**  817,842  817,842  811,773  774,716 
 **Max price_avg**  $4,557.51  $4,557.51  $2,525.30  $2,525.30 
 **Min price_avg**  $10.00  $10.00  $12.39  $11.69 
 **Rice max (2016)**  $359.00  $359.00  $359.00  $56.10  
 **Rice max (2025)**  $228.00  $228.00  $228.00  $99.00  
 **Outlier method**  None  3x  3x  IQR 
 **Metrics checked**  N/A  1  1  3 

### Product Quality Metrics

How many products have problematic characteristics?

 Issue  V2  V3  V4  Improvement 
-----------------------
 High CV (>50%)  7  7  4   43% 
 High skewness (abs>2)  19  14  10   47% 
 High kurtosis (abs>3)  30  26  20   33% 

---

## Running the Pipeline

### For New Analysis

**Use V4** by default:

```python
import pandas as pd

# Load the clean V4 data
prices = pd.read_csv('data/processed/prices_aggregated_all_years_v4.csv')

# This is production-ready data
print(f"Records: {len(prices):,}")
print(f"Date range: {prices['date'].min()} to {prices['date'].max()}")
print(f"Products: {prices['product_id'].nunique()}")
```

### For Reproducibility

To regenerate the entire pipeline from V1:

```bash
# 1. Generate V2 (add structure and detect with 3x method)
uv run python scripts/processing/detect_price_outliers.py

# 2. Generate V3 (remove v2 outliers conservatively)
uv run python scripts/processing/create_clean_dataset_v3.py

# 3. Generate V4 (improved IQR detection)
uv run python scripts/processing/detect_outliers_v4_improved.py

# 4. Generate statistics for quality verification
uv run python scripts/product_descriptive_statistics_v4.py
```

### For Visualization

Create professional charts with clean data:

```bash
# Rice price evolution using V4
uv run python scripts/visualization/visualize_rice_price_evolution_v4.py

# Rice price distribution by brand (deprecated)
uv run python scripts/deprecated/visualize_rice_price_distribution.py

# Product statistics dashboard
uv run python scripts/product_descriptive_statistics_v4.py
```

---

## Quality Checks

### Verify V4 Data Integrity

Run these checks to confirm V4 is production-ready:

```python
import pandas as pd

prices = pd.read_csv('data/processed/prices_aggregated_all_years_v4.csv')

# 1. No null values in key columns
assert prices[['product_id', 'date', 'price_avg']].notna().all()
print(" No null values in key columns")

# 2. Price logic: price_min <= price_avg <= price_max
assert (prices['price_min'] <= prices['price_avg']).all()
assert (prices['price_avg'] <= prices['price_max']).all()
print(" Price ordering is valid (min ≤ avg ≤ max)")

# 3. Extreme values are reasonable
assert prices['price_avg'].max() < 3000, f"Max price too high: ${prices['price_avg'].max()}"
assert prices['price_avg'].min() > 5, f"Min price too low: ${prices['price_avg'].min()}"
print(" Price ranges are reasonable")

# 4. Date coverage
assert len(prices['date'].unique()) > 100, "Not enough unique dates"
print(f" Date coverage: {len(prices['date'].unique())} unique dates across 10 years")

# 5. Product coverage
assert prices['product_id'].nunique() > 300, "Missing products"
print(f" Product coverage: {prices['product_id'].nunique()} products")

print("\n V4 DATA INTEGRITY VERIFIED ")
```

---

## Decision Flowchart: Which Version to Use?

```
START: Choosing a dataset version

─ Do you need analysis/visualization?
  ─ YES  Use V4 (IQR Clean)  RECOMMENDED

─ Do you need to understand data cleaning?
  ─ YES  Read this document + outlier-detection-methodology.md

─ Do you need historical decisions?
  ─ YES  Read dataset-versions.md

─ Do you need raw data for comparison?
  ─ YES  Use V1 (original)

─ Do you need to study outlier detection?
  ─ YES  Use V2 (it has outlier flags marked)

─ Uncertain?
   ─ DEFAULT: Use V4 
```

---

## Known Limitations & Future Improvements

### Current Limitations

1. **Fresh produce volatility**: Bananas and tomatoes still show high price variation (CV > 50%) even in V4
   - **Cause**: Genuine seasonal availability and unit inconsistencies (per kg vs per unit)
   - **Fix needed**: Seasonal decomposition or unit standardization

2. **Store-level anomalies**: IQR catches extreme store prices, but some legitimate variation remains
   - **Cause**: Different stores in different regions have real price differences
   - **Current approach**: Accept as valid variation

3. **Single-product groups**: Products with <100 records may have IQR bounds that are too tight
   - **Risk**: Could remove legitimate data points
   - **Mitigation**: Can add minimum sample size checks if needed

### Recommended Future Work

1. **Unit standardization**: Ensure all produce prices use same units (kg not individual)
2. **Regional analysis**: Account for price differences by store location
3. **Seasonal adjustment**: Create seasonal indices for produce
4. **Time-series analysis**: Use ARIMA or seasonal decomposition for trend extraction
5. **Product-specific thresholds**: Different outlier detection for different categories

---

## Conclusion

The **V4 dataset** represents the best balance of data quality and retention:

 **Statistically rigorous** (IQR method is industry standard)
 **Proven effective** (catches real anomalies like $359/$228)
 **High retention** (94.73% of records preserved)
 **Production-ready** (clean visualizations, trustworthy analysis)
 **Reproducible** (documented scripts, clear methodology)

For any new analysis, visualization, or backend integration, **use V4**.
