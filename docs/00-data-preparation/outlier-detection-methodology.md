# Outlier Detection Methodology

## Overview

This document explains the outlier detection methods used to clean the price dataset, comparing three approaches: simple threshold (3x), IQR, and alternative methods.

---

## Method 1: Simple Threshold (3x) - Used in V2/V3

### Approach
```
For each (category + time_window) group:
  1. Calculate median price
  2. Calculate thresholds:
     - Lower = median / 3
     - Upper = median * 3
  3. Flag records outside these bounds
```

### Formula
```
outlier if: price < (median / 3) OR price > (median * 3)
```

### Rationale
- Simple to understand and implement
- Symmetric around median (3x above = 1/3 below)
- Captures extreme deviations (3x = 200% deviation)
- Protects against erroneous entries

### Results (V2)
- **Outliers detected:** 6,069 (0.74%)
- **Effective for:** Extreme values like $4,557 for a $40-50 product
- **Misses:** The $228 and $359 anomalies (fell within 3x bounds for their groups)

### Problems
1. **Arbitrary threshold:** Why 3x and not 2x or 4x?
2. **Asymmetric in log space:** 3x above ≠ 3x below in relative terms
3. **Ignores distribution shape:** Same threshold for uniform and skewed distributions
4. **May be too conservative:** For stable products, 3x might miss real errors
5. **Single metric:** Only checks `price_avg`, ignores `price_min`/`price_max`

### When to Use
- Quick initial screening
- When you want to be conservative (keep most data)
- For products with stable, known price ranges

---

## Method 2: Interquartile Range (IQR) - Used in V4 (RECOMMENDED)

### Approach
```
For each (category + time_window) group:
  1. Calculate Q1 (25th percentile) and Q3 (75th percentile)
  2. Calculate IQR = Q3 - Q1
  3. Calculate bounds:
     - Lower = Q1 - 1.5 * IQR
     - Upper = Q3 + 1.5 * IQR
  4. Flag records outside these bounds
```

### Formula
```
outlier if: price < (Q1 - 1.5*IQR) OR price > (Q3 + 1.5*IQR)
```

### Rationale
- **Statistical standard:** IQR method is the industry-standard for box plots
- **Distribution-aware:** Adapts to actual data spread
- **Robust:** Uses percentiles, not affected by one extreme value
- **Multiple metrics:** Can check `price_avg`, `price_min`, `price_max` separately
- **Data-driven:** Thresholds emerge from data, not arbitrary constants

### Results (V4)
- **Outliers detected:** 43,126 (5.27%)
- **Effective for:** Normal deviations AND extreme store prices
- **Successfully caught:** Both $228 and $359 anomalies

### Advantages
1. **Statistically rigorous:** Widely accepted in statistics
2. **Adaptive:** Different products get different thresholds
3. **Comprehensive:** Checks all three price metrics
4. **Asymmetric:** Naturally handles skewed distributions
5. **Interpretable:** Based on data percentiles, not magic numbers

### Disadvantages
1. **Removes more data:** 5.27% vs 0.74%
2. **Less transparent:** Harder to explain why a record was removed
3. **May over-remove:** Legitimate but unusual variations could be flagged
4. **Requires grouped analysis:** Must analyze within category+time_window groups

### When to Use
- Professional analysis and research
- When data quality is critical
- For regulatory or compliance reporting
- When statistical rigor is required
- **Default choice for this project**

### Implementation Details

```python
import numpy as np

for (category, year_month), group in data.groupby(['category', 'year_month']):
    # Check all three price metrics
    for metric in ['price_avg', 'price_min', 'price_max']:
        prices = group[metric].values

        q1 = np.percentile(prices, 25)
        q3 = np.percentile(prices, 75)
        iqr = q3 - q1

        lower_bound = q1 - 1.5 * iqr
        upper_bound = q3 + 1.5 * iqr

        outliers = (prices < lower_bound)  (prices > upper_bound)
```

---

## Alternative Methods

### Method 3: Z-Score (Standardized Deviation)

**Formula:**
```
z_score = (value - mean) / std_dev
outlier if: z_score > 3
```

**Pros:**
- Simple to calculate
- Based on standard deviation
- Common in statistical analysis

**Cons:**
- Assumes normal distribution
- Sensitive to extreme values
- Requires sufficient sample size
- Not ideal for this data (many products are skewed)

**When to use:** For normally-distributed, high-frequency products

---

### Method 4: Percentiles (P5-P95)

**Approach:**
```
Exclude bottom 5% and top 5% of prices in each group
```

**Pros:**
- Simple and deterministic
- Removes exact percentage of data
- Easy to explain

**Cons:**
- Removes valid data even when no outliers exist
- Arbitrary percentile choice
- Ignores where the actual outliers are

**When to use:** When you know exactly what percentage to keep

---

### Method 5: Modified Z-Score (Using MAD)

**Formula:**
```
median_absolute_deviation = median(value - median)
modified_z = 0.6745 * (value - median) / MAD
outlier if: modified_z > 3.5
```

**Pros:**
- Robust to extreme values
- More reliable than standard Z-score
- Less affected by data distribution

**Cons:**
- Less well-known
- Harder to implement correctly
- Still assumes roughly normal distribution

**When to use:** For robust outlier detection when data has some outliers

---

## Comparison of Methods

 Method  V2 (3x)  V4 (IQR)  Z-Score  P5-P95  MAD 
---------------------------------------------------
 **Outliers found**  6,069  43,126  ~25,000  40,892  ~30,000 
 **% removed**  0.74%  5.27%  ~3%  5%  ~3.7% 
 **Statistical rigor**  Low  ⭐⭐⭐⭐⭐  Medium  Low  High 
 **Interpretable**           
 **Catches $228/$359**           
 **Handles skew**           
 **Conservative**    Medium  Medium  Low  Medium 

---

## Decision Process: Which Method to Use?

```
START
  
Is this for research/regulatory?
  YES  Use IQR (V4) 
  NO
    
  Is data heavily skewed (produce, fresh items)?
    YES  Use IQR or MAD
    NO
      
    Do you need to remove ONLY extreme errors?
      YES  Use 3x method (V3)
      NO
        
    Do you know exact % to keep?
      YES  Use Percentiles
      NO  Use IQR (V4) 
```

---

## The Specific Anomaly: Why IQR Caught It

### The Problem Record
- **Date:** 2016-07-20
- **Product:** Green Chef rice (ID 16)
- **price_min:** $26.00 (one store)
- **price_max:** $359.00 (ONE store - this is the error!)
- **price_avg:** $44.80 (reasonable average)
- **price_median:** $38.16

### Why 3x Method Failed
```
Median price group: $38.16
3x bounds: $12.72 - $114.48
price_avg: $44.80  WITHIN bounds  NOT flagged
```

The average price was normal, so 3x threshold passed it through.

### Why IQR Method Succeeded
```
For price_max metric in this group:
Q1 = $30
Q3 = $50
IQR = $20
Lower = $30 - 1.5*20 = $0
Upper = $50 + 1.5*20 = $80

price_max = $359  OUTSIDE upper bound  FLAGGED!
```

IQR checked the actual `price_max` column, which captured the single store selling at $359.

---

## Real-World Impact

### For Rice (the example)

**V2 with 3x method:**
- Records: 21,347
- Removed: 0
- Max price in 2016: $359.00  Visible in charts
- Max price in 2025: $228.00  Visible in charts

**V4 with IQR method:**
- Records: 20,467
- Removed: 880
- Max price in 2016: $56.10  Clean
- Max price in 2025: $99.00  Clean
- Visual improvement: Charts look professional, not distorted by outliers

---

## Conclusion

**For Open Price UY project:** V4 (IQR method) is the optimal choice because:

1. It's statistically sound and widely recognized
2. It successfully identifies real data errors
3. It maintains 94.73% of records (good retention)
4. It makes visualizations clearer and more professional
5. It's reproducible and defensible in analysis

The trade-off of removing 5.27% of data is well worth the improved data quality.
