# Dataset Versions: V1 through V4

## Overview

This document describes the evolution of the price dataset through multiple cleaning and processing stages. Each version builds upon the previous one with improved data quality.

## Version History

### V1: Original Raw Data
**File:** `prices_aggregated_all_years.csv` (42 MB, 817,842 records)

**Characteristics:**
- Raw data from yearly consolidated files (2016-2025)
- 10 columns: `product_id, date, price_min, price_max, price_avg, price_median, price_std, store_count, offer_count, offer_percentage`
- Contains extreme outliers: max price $4,557.51, min price $10.00
- No quality flags or categorization

**Issues:**
- Extreme values from data entry errors or unit inconsistencies
- No product category information
- No time window information for analysis
- Difficult to identify which records are problematic

### V2: Labeled with Category and Outlier Detection (3x Method)
**File:** `prices_aggregated_all_years_v2.csv` (64 MB, 817,842 records)

**Added Columns:**
- `year_month` - YYYY-MM format for time windowing
- `category` - Product category from catalog
- `outlier` - Binary flag (0=normal, 1=anomaly)

**Detection Method:**
- Groups by: `category + year_month`
- Calculates: median price for each group
- Flags as outlier if: `price_avg < median/3` OR `price_avg > median*3`
- **Outliers detected:** 6,069 (0.74%)

**Advantages:**
- Enriches data with category information
- Labels problematic records without removing them
- Conservative approach (only catches extreme outliers)
- Easy to debug and verify detections

**Disadvantages:**
- Still contains $359 and $228 anomalies
- May miss realistic but unusual variations
- 3x threshold is arbitrary

**Key Finding:** $359 anomaly on 2016-07-20 (Green Chef rice, single store price)

### V3: Clean Dataset (3x Method, Data Removed)
**File:** `prices_aggregated_all_years_v3.csv` (61.1 MB, 811,773 records)

**Process:**
- Started with V2
- Removed all records where `outlier = 1`
- Dropped the `outlier` column
- Retained 12 columns: original 10 + `year_month` + `category`

**Results:**
- **Records removed:** 6,069 (0.74%)
- **Records retained:** 811,773 (99.26%)
- **Max price now:** $2,525.30 (was $4,557.51)
- **Min price now:** $12.39 (was $10.00)

**Data Quality Improvement:**
- Price volatility (CV) decreased by ~5%
- Max price reduction: 44.6%
- But still contains $228 anomaly (2025-08-28, Green Chef)

**Issue:** The $228 maximum wasn't caught by 3x method because it fit within the group bounds

### V4: Improved Detection (IQR Method, Recommended)
**File:** `prices_aggregated_all_years_v4.csv` (61.1 MB, 774,716 records)

**Detection Method:**
- Groups by: `category + year_month`
- For each group, calculates on `price_avg`, `price_min`, `price_max`:
  - Q1 (25th percentile)
  - Q3 (75th percentile)
  - IQR = Q3 - Q1
  - Lower bound = Q1 - 1.5 × IQR
  - Upper bound = Q3 + 1.5 × IQR
- Flags as outlier if ANY metric outside bounds
- **Outliers detected:** 43,126 (5.27%)

**Results:**
- **Records removed:** 43,126 (5.27%)
- **Records retained:** 774,716 (94.73%)
- **Max price now:** $2,525.30 (similar to V3)
- **Min price now:** $11.69

**Comparison with V3:**
 Metric  V3  V4  Change 
---------------------------
 Rice max (2016)  $359.00  $56.10   Caught 
 Rice max (2025)  $228.00  $99.00   Caught 
 Records removed  6,069  43,126  +37,057 
 Products with high CV  7  4   Improved 
 Products with high skewness  19  10   Improved 

**Advantages:**
- **Statistically robust:** IQR is the standard statistical method for outlier detection
- **Catches real anomalies:** Successfully removed the $359 and $228 extremes
- **Adaptive thresholds:** Uses actual data distribution, not fixed multiples
- **Data retained:** 94.73% - aggressive but reasonable
- **Normal distributions:** Products now show more normal skewness/kurtosis

**Disadvantages:**
- Removes more data (5.27% vs 0.74%)
- May remove some legitimate price variations
- More aggressive than needed for some use cases

---

## Recommendation: Use V4

**V4 is recommended for analysis because:**

1. **Statistical rigor:** IQR method is the industry standard for outlier detection
2. **Catches real errors:** Successfully identified and removed the $359 and $228 anomalies
3. **Good data retention:** 94.73% is acceptable balance
4. **Cleaner distributions:** Significantly fewer products with extreme skewness/kurtosis
5. **Reliable trends:** Historical trends (rice inflation, etc.) remain identical
6. **Scalable:** IQR method is more robust than arbitrary 3x thresholds

---

## Case Study: Rice Price Anomaly

This case demonstrates why V4 is better:

**The Anomaly:** 2016-07-20, Green Chef rice had `price_max = $359.00` while `price_avg = $44.80`

**Detection Results:**
- **V2 (3x method):**  Did NOT flag as outlier (price_avg was normal)
- **V3 (Remove from V2):**  Did NOT remove (not in outlier column)
- **V4 (IQR method):**  Flagged as outlier (price_max far outside IQR bounds)

**Why V4 succeeded:** IQR checks all three price metrics (`price_min`, `price_max`, `price_avg`), not just `price_avg`. One extreme store price made `price_max` an outlier even though the average was reasonable.

---

## Data Quality Summary by Version

 Aspect  V1  V2  V3  V4 
-----------------------------
 Records  817,842  817,842  811,773  774,716 
 Max price_avg  $4,557  $4,557  $2,525  $2,525 
 High CV products  N/A  7  7  4 
 High skewness  N/A  19  14  10 
 High kurtosis  N/A  30  26  20 
 Recommended?         

---

## Migration Guide

To migrate analysis code from V3 to V4:

```python
# Old (V3)
prices = pd.read_csv('data/processed/prices_aggregated_all_years_v3.csv')

# New (V4) - Recommended
prices = pd.read_csv('data/processed/prices_aggregated_all_years_v4.csv')
```

No changes needed to downstream code - the structure is identical, only cleaner.

---

## Future Improvements

1. **Investigate produce outliers:** Fresh produce (bananas, tomatoes) still shows high CV even in V4 - may need seasonal adjustment or unit standardization
2. **Time-series analysis:** Could use seasonal decomposition for produce to separate trend from seasonal variation
3. **Product-specific thresholds:** Some products may warrant different outlier thresholds based on their category
4. **Validation set:** Create a labeled set of "known good" and "known bad" records to validate detection methods
