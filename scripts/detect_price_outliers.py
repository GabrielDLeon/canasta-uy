"""
Detects price anomalies in product data.

Method:
1. Groups products by exact category (from product_groups_by_category.json)
2. Creates time windows (month-year combinations)
3. For each category + time window group:
   - Calculates median price across all products in that group
   - Flags prices that are 3x higher or 3x lower as outliers
4. Creates 'outlier' column (0=normal, 1=anomaly)
5. Outputs cleaned data as v2 CSV file

Logic:
- Different brands of the same product category should have similar prices
- Time windows prevent inflation distortion (comparing Jan 2016 vs Jan 2023)
- 3x threshold captures significant price deviations while allowing brand variance
"""

import pandas as pd
import numpy as np
import json
from pathlib import Path
from datetime import datetime

print("="*80)
print("PRICE ANOMALY DETECTION SCRIPT")
print("="*80)

# Load data
print("\n1. Loading data...")
prices_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years.csv"
groups_path = Path(__file__).parent.parent / "data" / "processed" / "product_groups_by_category.json"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(prices_path)
products_df = pd.read_csv(products_path)

with open(groups_path, 'r', encoding='utf-8') as f:
    product_groups = json.load(f)

print(f"  - Prices: {len(prices_df):,} records")
print(f"  - Products: {len(products_df):,} products")
print(f"  - Categories: {len(product_groups['categories'])} groups")

# Convert date to datetime
prices_df['date'] = pd.to_datetime(prices_df['date'])

# Create time window (year-month)
print("\n2. Creating time windows (month-year)...")
prices_df['year_month'] = prices_df['date'].dt.to_period('M')

unique_windows = prices_df['year_month'].nunique()
print(f"  - Total time windows: {unique_windows}")
print(f"  - Date range: {prices_df['date'].min().date()} to {prices_df['date'].max().date()}")

# Map product_id to category
print("\n3. Mapping products to categories...")
product_to_category = {}
for category, product_ids in product_groups['categories'].items():
    for pid in product_ids:
        product_to_category[pid] = category

prices_df['category'] = prices_df['product_id'].map(product_to_category)

# Count how many products couldn't be mapped
unmapped = prices_df['category'].isna().sum()
if unmapped > 0:
    print(f"  ⚠ Warning: {unmapped} records couldn't be mapped to a category")
else:
    print(f"  ✓ All {len(prices_df):,} records mapped to categories")

# Initialize outlier column
prices_df['outlier'] = 0

# Detect outliers
print("\n4. Detecting outliers (3x threshold)...")
outlier_count = 0
group_count = 0

# Group by category and time window
for (category, year_month), group in prices_df.groupby(['category', 'year_month']):
    group_count += 1

    if group_count % 1000 == 0:
        print(f"  - Processed {group_count:,} groups ({outlier_count:,} outliers found)")

    # Calculate median price in this group
    median_price = group['price_avg'].median()

    # Skip if median is 0 or NaN
    if pd.isna(median_price) or median_price == 0:
        continue

    # Define outlier thresholds (3x higher or lower)
    lower_threshold = median_price / 3
    upper_threshold = median_price * 3

    # Find outliers in this group
    outlier_mask = (group['price_avg'] < lower_threshold) | (group['price_avg'] > upper_threshold)

    # Mark outliers in the main dataframe
    outlier_indices = group[outlier_mask].index
    prices_df.loc[outlier_indices, 'outlier'] = 1

    outlier_count += len(outlier_indices)

print(f"  ✓ Processed {group_count:,} groups")
print(f"  ✓ Found {outlier_count:,} outliers")

# Calculate statistics
print("\n5. Outlier Statistics")
print("="*80)

outlier_pct = (outlier_count / len(prices_df)) * 100
print(f"Total records analyzed: {len(prices_df):,}")
print(f"Outliers found: {outlier_count:,} ({outlier_pct:.2f}%)")
print(f"Normal records: {len(prices_df) - outlier_count:,} ({100-outlier_pct:.2f}%)")

# Show outliers by category
print("\n6. Top 10 Categories with Most Outliers")
print("="*80)

outlier_by_category = prices_df[prices_df['outlier'] == 1].groupby('category').size().sort_values(ascending=False)
for i, (category, count) in enumerate(outlier_by_category.head(10).items(), 1):
    pct = (count / len(prices_df[prices_df['category'] == category])) * 100
    print(f"{i:2d}. {category:50s} → {count:5d} outliers ({pct:5.1f}%)")

# Show outliers by year-month
print("\n7. Time Windows with Most Outliers")
print("="*80)

outlier_by_window = prices_df[prices_df['outlier'] == 1].groupby('year_month').size().sort_values(ascending=False)
for i, (window, count) in enumerate(outlier_by_window.head(10).items(), 1):
    total_in_window = len(prices_df[prices_df['year_month'] == window])
    pct = (count / total_in_window) * 100
    print(f"{i:2d}. {str(window):10s} → {count:5d} outliers ({pct:5.1f}%)")

# Save to v2 file
print("\n8. Saving cleaned dataset...")
output_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v2.csv"

# Keep only necessary columns for v2
output_df = prices_df.copy()
output_df['year_month'] = output_df['year_month'].astype(str)

output_df.to_csv(output_path, index=False)

print(f"  ✓ File saved: {output_path}")
print(f"  - Shape: {output_df.shape[0]:,} rows × {output_df.shape[1]} columns")
print(f"  - New columns: 'category', 'year_month', 'outlier'")

# Show sample of data
print("\n9. Sample Data (first 5 records with outliers)")
print("="*80)

sample_outliers = prices_df[prices_df['outlier'] == 1].head(5)
if len(sample_outliers) > 0:
    for idx, row in sample_outliers.iterrows():
        print(f"\nProduct ID: {row['product_id']}")
        print(f"  Category: {row['category']}")
        print(f"  Date: {row['date'].date()}")
        print(f"  Price (avg): ${row['price_avg']:.2f}")
        print(f"  Year-Month: {row['year_month']}")
        print(f"  Status: {'OUTLIER' if row['outlier'] == 1 else 'NORMAL'}")

print("\n" + "="*80)
print("✓ ANOMALY DETECTION COMPLETE")
print("="*80)
