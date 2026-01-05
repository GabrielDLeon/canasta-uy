"""
Creates v4 dataset with improved outlier detection using IQR method.

Method: Interquartile Range (IQR)
- For each category + time window group:
  - Calculate Q1 (25th percentile), Q3 (75th percentile)
  - IQR = Q3 - Q1
  - Lower bound = Q1 - 1.5 * IQR
  - Upper bound = Q3 + 1.5 * IQR
  - Flag any price_avg, price_min, or price_max outside bounds

This is more aggressive than 3x method but catches real anomalies like $359 and $228.
"""

import pandas as pd
import numpy as np
import json
from pathlib import Path

print("="*80)
print("CREATING V4 DATASET WITH IMPROVED OUTLIER DETECTION (IQR METHOD)")
print("="*80)

# Load v2 (with category and year_month columns)
v2_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v2.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_v2 = pd.read_csv(v2_path)
products_df = pd.read_csv(products_path)

prices_v2['date'] = pd.to_datetime(prices_v2['date'])

print(f"\n1. Loading v2 dataset...")
print(f"  - Total records: {len(prices_v2):,}")

# Initialize outlier column
prices_v2['outlier_v4'] = 0

# Detect outliers using IQR method
print(f"\n2. Detecting outliers using IQR method...")
outlier_count = 0
group_count = 0

# Group by category and time window
for (category, year_month), group in prices_v2.groupby(['category', 'year_month']):
    group_count += 1

    if group_count % 1000 == 0:
        print(f"  - Processed {group_count:,} groups ({outlier_count:,} outliers found)")

    # Check price_avg
    price_avg = group['price_avg'].values
    q1_avg = np.percentile(price_avg, 25)
    q3_avg = np.percentile(price_avg, 75)
    iqr_avg = q3_avg - q1_avg
    lower_avg = q1_avg - 1.5 * iqr_avg
    upper_avg = q3_avg + 1.5 * iqr_avg

    # Check price_min
    price_min = group['price_min'].values
    q1_min = np.percentile(price_min, 25)
    q3_min = np.percentile(price_min, 75)
    iqr_min = q3_min - q1_min
    lower_min = q1_min - 1.5 * iqr_min
    upper_min = q3_min + 1.5 * iqr_min

    # Check price_max
    price_max = group['price_max'].values
    q1_max = np.percentile(price_max, 25)
    q3_max = np.percentile(price_max, 75)
    iqr_max = q3_max - q1_max
    lower_max = q1_max - 1.5 * iqr_max
    upper_max = q3_max + 1.5 * iqr_max

    # Find outliers: flag if ANY price metric is outside IQR bounds
    outlier_mask = (
        (group['price_avg'] < lower_avg) | (group['price_avg'] > upper_avg) |
        (group['price_min'] < lower_min) | (group['price_min'] > upper_min) |
        (group['price_max'] < lower_max) | (group['price_max'] > upper_max)
    )

    # Mark outliers
    outlier_indices = group[outlier_mask].index
    prices_v2.loc[outlier_indices, 'outlier_v4'] = 1

    outlier_count += len(outlier_indices)

print(f"  ✓ Processed {group_count:,} groups")
print(f"  ✓ Found {outlier_count:,} outliers")

# Calculate statistics
print(f"\n3. Outlier Statistics")
print("="*80)

outlier_pct = (outlier_count / len(prices_v2)) * 100
print(f"Total records analyzed: {len(prices_v2):,}")
print(f"Outliers found (IQR): {outlier_count:,} ({outlier_pct:.2f}%)")
print(f"Normal records: {len(prices_v2) - outlier_count:,} ({100-outlier_pct:.2f}%)")

# Compare with v3 (3x method)
v3_outliers = prices_v2[prices_v2['outlier'] == 1]
v4_new_outliers = prices_v2[(prices_v2['outlier_v4'] == 1) & (prices_v2['outlier'] == 0)]
v4_removed = prices_v2[(prices_v2['outlier_v4'] == 1) & (prices_v2['outlier'] == 1)]

print(f"\nComparison with v3 (3x method):")
print(f"  V3 outliers: {len(v3_outliers):,}")
print(f"  V4 outliers (IQR): {outlier_count:,}")
print(f"  New outliers detected: {len(v4_new_outliers):,}")
print(f"  Previously detected: {len(v4_removed):,}")

# Show top products with NEW outliers in v4
print(f"\n4. Top 10 NEW Outliers Detected by V4 (not in V3)")
print("="*80)

new_outliers = v4_new_outliers.copy()
top_new = new_outliers.groupby(['category', 'product_id']).size().sort_values(ascending=False).head(10)

for (category, product_id), count in top_new.items():
    prod = products_df[products_df['product_id'] == product_id]
    if len(prod) > 0:
        prod_name = prod.iloc[0]['name']
        print(f"  {category:40s} | {count:4d} | {prod_name[:50]}")

# Show the specific case we know about ($359 and $228)
print(f"\n5. Verification: Did V4 catch the $359 and $228 anomalies?")
print("="*80)

rice_outliers_v4 = prices_v2[(prices_v2['category'] == 'Arroz blanco') & (prices_v2['outlier_v4'] == 1)]
print(f"\nArroz blanco outliers in V4: {len(rice_outliers_v4)}")

extreme_359 = prices_v2[(prices_v2['price_max'] >= 350)]
if len(extreme_359) > 0 and extreme_359.iloc[0]['outlier_v4'] == 1:
    print(f"  ✓ CAUGHT: $359 anomaly is marked as outlier in V4")
else:
    print(f"  ✗ MISSED: $359 anomaly is NOT marked as outlier in V4")

extreme_228 = prices_v2[(prices_v2['price_max'] >= 220) & (prices_v2['price_max'] < 350)]
if len(extreme_228) > 0 and extreme_228.iloc[0]['outlier_v4'] == 1:
    print(f"  ✓ CAUGHT: $228 anomaly is marked as outlier in V4")
else:
    print(f"  ✗ MISSED: $228 anomaly is NOT marked as outlier in V4")

# Create v4 by removing all outliers
print(f"\n6. Creating V4 dataset (removing all IQR outliers)...")
prices_v4 = prices_v2[prices_v2['outlier_v4'] == 0].copy()

# Drop both outlier columns
prices_v4 = prices_v4.drop(['outlier', 'outlier_v4'], axis=1)

print(f"  - V4 records: {len(prices_v4):,}")
print(f"  - Retention rate: {len(prices_v4)/len(prices_v2)*100:.2f}%")

# Save v4
output_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4.csv"
prices_v4.to_csv(output_path, index=False)
print(f"\n✓ V4 dataset saved: {output_path}")

# Summary statistics
print(f"\n7. Data Quality Improvements (V2 → V3 → V4)")
print("="*80)

print(f"\nPrice_avg statistics:")
print(f"  V2: ${prices_v2['price_avg'].min():.2f} - ${prices_v2['price_avg'].max():.2f}")
print(f"  V4: ${prices_v4['price_avg'].min():.2f} - ${prices_v4['price_avg'].max():.2f}")

print(f"\nPrice_max statistics:")
print(f"  V2: ${prices_v2['price_max'].min():.2f} - ${prices_v2['price_max'].max():.2f}")
print(f"  V4: ${prices_v4['price_max'].min():.2f} - ${prices_v4['price_max'].max():.2f}")

print(f"\nPrice_min statistics:")
print(f"  V2: ${prices_v2['price_min'].min():.2f} - ${prices_v2['price_min'].max():.2f}")
print(f"  V4: ${prices_v4['price_min'].min():.2f} - ${prices_v4['price_min'].max():.2f}")

print(f"\n" + "="*80)
print("✓ V4 DATASET CREATED SUCCESSFULLY WITH IMPROVED OUTLIER DETECTION")
print("="*80)
