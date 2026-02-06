"""
Comprehensive descriptive statistics for all products.

Usage:
    uv run python scripts/product_descriptive_statistics_v4.py [prices_file] [products_file]

Examples:
    uv run python scripts/product_descriptive_statistics_v4.py
        Uses default: v4.csv and products_catalog.csv

    uv run python scripts/product_descriptive_statistics_v4.py v4_clean
        Uses: v4_clean.csv and products_catalog.csv

    uv run python scripts/product_descriptive_statistics_v4.py v4_clean products_catalog_clean
        Uses: v4_clean.csv and products_catalog_clean.csv
"""

import pandas as pd
import numpy as np
from pathlib import Path
from scipy import stats
import sys

# Parse arguments
prices_arg = sys.argv[1] if len(sys.argv) > 1 else "v4"
products_arg = sys.argv[2] if len(sys.argv) > 2 else "products_catalog"

print("="*100)
print("PRODUCT DESCRIPTIVE STATISTICS")
print("="*100)

# Build full paths - intelligently construct filenames
data_dir = Path(__file__).parent.parent / "data" / "processed"

# Handle prices file
if prices_arg.startswith("prices_"):
    v4_path = data_dir / f"{prices_arg}.csv"
elif prices_arg == "v4" or prices_arg == "v4_clean":
    v4_path = data_dir / f"prices_aggregated_all_years_{prices_arg}.csv"
else:
    v4_path = data_dir / f"{prices_arg}.csv"

# Handle products file
if products_arg.startswith("products_"):
    products_path = data_dir / f"{products_arg}.csv"
else:
    products_path = data_dir / f"products_{products_arg}.csv" if products_arg != "catalog" else data_dir / "products_catalog.csv"

print(f"\nLoading prices from: {v4_path.name}")
print(f"Loading products from: {products_path.name}")

# Validate files exist
if not v4_path.exists():
    print(f"❌ ERROR: Prices file not found: {v4_path}")
    sys.exit(1)
if not products_path.exists():
    print(f"❌ ERROR: Products file not found: {products_path}")
    sys.exit(1)

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

print(f"\nLoaded {len(prices_df):,} price records for {len(products_df)} products")

# Calculate statistics for each product
stats_list = []

for product_id in sorted(products_df['product_id'].unique()):
    prod_data = prices_df[prices_df['product_id'] == product_id]

    if len(prod_data) == 0:
        continue

    # Get product info
    prod_info = products_df[products_df['product_id'] == product_id].iloc[0]

    prices = prod_data['price_avg'].values

    # Basic statistics
    min_price = prices.min()
    max_price = prices.max()
    mean_price = prices.mean()
    median_price = np.median(prices)
    std_price = prices.std()
    range_price = max_price - min_price

    # Mode (most common price)
    try:
        mode_price = stats.mode(prices, keepdims=True).mode[0]
        mode_count = stats.mode(prices, keepdims=True).count[0]
    except:
        mode_price = np.nan
        mode_count = 0

    # Percentiles
    p10 = np.percentile(prices, 10)
    p25 = np.percentile(prices, 25)
    p75 = np.percentile(prices, 75)
    p90 = np.percentile(prices, 90)

    # IQR (Interquartile Range)
    iqr = p75 - p25

    # Coefficient of Variation (CV)
    cv = (std_price / mean_price * 100) if mean_price != 0 else 0

    # Frequency
    frequency = len(prod_data)

    # Skewness and Kurtosis
    skewness = stats.skew(prices)
    kurtosis = stats.kurtosis(prices)

    # Coefficient of Range (Range / Mean)
    coef_range = (range_price / mean_price * 100) if mean_price != 0 else 0

    stats_list.append({
        'product_id': product_id,
        'category': prod_info['category'],
        'brand': prod_info['brand'],
        'name': prod_info['name'],
        'frequency': frequency,
        'min': min_price,
        'max': max_price,
        'range': range_price,
        'mean': mean_price,
        'median': median_price,
        'mode': mode_price,
        'mode_count': mode_count,
        'std': std_price,
        'cv_pct': cv,
        'coef_range_pct': coef_range,
        'p10': p10,
        'p25': p25,
        'p75': p75,
        'p90': p90,
        'iqr': iqr,
        'skewness': skewness,
        'kurtosis': kurtosis,
    })

stats_df = pd.DataFrame(stats_list)

print(f"Calculated statistics for {len(stats_df)} products")

# SAVE TO CSV (dynamic name based on input file)
# Extract version suffix from prices file
if "v4_clean" in v4_path.name:
    suffix = "v4_clean"
elif "v4" in v4_path.name:
    suffix = "v4"
elif "v3" in v4_path.name:
    suffix = "v3"
else:
    suffix = prices_arg

output_name = f"product_statistics_{suffix}.csv"
output_csv = data_dir / output_name
stats_df.to_csv(output_csv, index=False)
print(f"\n✓ Statistics saved to: {output_csv}")

# IDENTIFY ANOMALOUS PRODUCTS
print("\n" + "="*100)
print("PRODUCTS WITH ANOMALOUS CHARACTERISTICS")
print("="*100)

# High Coefficient of Variation (>50% = high price volatility)
high_cv = stats_df[stats_df['cv_pct'] > 50].sort_values('cv_pct', ascending=False)
print(f"\n1. HIGH PRICE VOLATILITY (CV > 50%): {len(high_cv)} products")
print("-" * 100)
if len(high_cv) > 0:
    for idx, row in high_cv.head(20).iterrows():
        print(f"   {row['product_id']:3d} | {row['cv_pct']:6.2f}% CV | ${row['min']:7.2f}-${row['max']:7.2f} | {row['name'][:60]}")
else:
    print("   (None found - data is very clean!)")

# Products with very few records (potential data issues)
few_records = stats_df[stats_df['frequency'] < 100].sort_values('frequency')
print(f"\n2. PRODUCTS WITH FEW RECORDS (<100): {len(few_records)} products")
print("-" * 100)
if len(few_records) > 0:
    for idx, row in few_records.head(20).iterrows():
        print(f"   {row['product_id']:3d} | {row['frequency']:4d} records | CV: {row['cv_pct']:6.2f}% | {row['name'][:60]}")
else:
    print("   (None found)")

# Products with unusual distributions (high skewness)
high_skew = stats_df[stats_df['skewness'].abs() > 2].sort_values('skewness', ascending=False, key=abs)
print(f"\n3. HIGH SKEWNESS (|skewness| > 2): {len(high_skew)} products")
print("-" * 100)
if len(high_skew) > 0:
    for idx, row in high_skew.head(15).iterrows():
        direction = "Right-skewed" if row['skewness'] > 0 else "Left-skewed"
        print(f"   {row['product_id']:3d} | Skew: {row['skewness']:7.2f} ({direction}) | {row['name'][:60]}")
else:
    print("   (None found - distributions are now normal!)")

# Products with high kurtosis (unusual tails)
high_kurtosis = stats_df[stats_df['kurtosis'].abs() > 3].sort_values('kurtosis', ascending=False, key=abs)
print(f"\n4. HIGH KURTOSIS (|kurtosis| > 3): {len(high_kurtosis)} products")
print("-" * 100)
if len(high_kurtosis) > 0:
    for idx, row in high_kurtosis.head(15).iterrows():
        print(f"   {row['product_id']:3d} | Kurtosis: {row['kurtosis']:7.2f} | {row['name'][:60]}")
else:
    print("   (None found - tails are now normal!)")

# Overall product quality assessment
print("\n" + "="*100)
print("PRODUCT QUALITY ASSESSMENT")
print("="*100)

# Define quality score based on multiple factors
stats_df['quality_issues'] = 0
stats_df.loc[stats_df['cv_pct'] > 50, 'quality_issues'] += 1
stats_df.loc[stats_df['frequency'] < 100, 'quality_issues'] += 1
stats_df.loc[stats_df['skewness'].abs() > 2, 'quality_issues'] += 1
stats_df.loc[stats_df['kurtosis'].abs() > 3, 'quality_issues'] += 1

problematic = stats_df[stats_df['quality_issues'] > 0].sort_values('quality_issues', ascending=False)

print(f"\nProducts with quality issues: {len(problematic)} out of {len(stats_df)}")

if len(problematic) > 0 and len(problematic) <= 20:
    print("\nProblematic products:")
    print("-" * 100)

    for idx, row in problematic.iterrows():
        issues = []
        if row['cv_pct'] > 50:
            issues.append(f"High CV ({row['cv_pct']:.1f}%)")
        if row['frequency'] < 100:
            issues.append(f"Few records ({row['frequency']})")
        if row['skewness'] > 2:
            issues.append(f"Right-skewed ({row['skewness']:.1f})")
        if row['skewness'] < -2:
            issues.append(f"Left-skewed ({row['skewness']:.1f})")
        if row['kurtosis'] > 3:
            issues.append(f"High kurtosis ({row['kurtosis']:.1f})")

        print(f"\n{row['product_id']:3d}. {row['name'][:70]}")
        print(f"     Issues: {' | '.join(issues)}")
        print(f"     Stats: Min=${row['min']:.2f} Max=${row['max']:.2f} Mean=${row['mean']:.2f} Std=${row['std']:.2f}")
else:
    if len(problematic) == 0:
        print("\n✓ NO QUALITY ISSUES FOUND! Dataset is extremely clean.")
    else:
        print(f"\nTop 20 problematic products (showing {len(problematic)} total with issues):")
        for idx, row in problematic.head(20).iterrows():
            issues = []
            if row['cv_pct'] > 50:
                issues.append(f"High CV")
            if row['frequency'] < 100:
                issues.append(f"Few recs")
            if row['skewness'] > 2 or row['skewness'] < -2:
                issues.append(f"Skew:{row['skewness']:.1f}")
            if row['kurtosis'] > 3:
                issues.append(f"Kurt:{row['kurtosis']:.1f}")
            print(f"  {row['product_id']:3d}. {row['name'][:60]} | {' '.join(issues)}")

# SUMMARY STATISTICS
print("\n" + "="*100)
print("DATASET-WIDE STATISTICS")
print("="*100)

print("\nCoefficient of Variation (CV%) - Price Volatility:")
print(f"  Mean CV across products: {stats_df['cv_pct'].mean():.2f}%")
print(f"  Median CV: {stats_df['cv_pct'].median():.2f}%")
print(f"  Min CV: {stats_df['cv_pct'].min():.2f}%")
print(f"  Max CV: {stats_df['cv_pct'].max():.2f}%")
print(f"  Std of CV: {stats_df['cv_pct'].std():.2f}%")

print("\nFrequency (Record Count):")
print(f"  Mean records per product: {stats_df['frequency'].mean():.0f}")
print(f"  Median records per product: {stats_df['frequency'].median():.0f}")
print(f"  Min records: {stats_df['frequency'].min()}")
print(f"  Max records: {stats_df['frequency'].max()}")

print("\nPrice Statistics (Mean across products):")
print(f"  Average minimum price: ${stats_df['min'].mean():.2f}")
print(f"  Average maximum price: ${stats_df['max'].mean():.2f}")
print(f"  Average price mean: ${stats_df['mean'].mean():.2f}")
print(f"  Average std deviation: ${stats_df['std'].mean():.2f}")

# Distribution quality
print("\nSkewness Distribution (Normal ≈ 0):")
print(f"  Products with |skewness| < 0.5: {len(stats_df[stats_df['skewness'].abs() < 0.5])} (normal)")
print(f"  Products with |skewness| 0.5-2: {len(stats_df[(stats_df['skewness'].abs() >= 0.5) & (stats_df['skewness'].abs() <= 2)])} (moderately skewed)")
print(f"  Products with |skewness| > 2: {len(stats_df[stats_df['skewness'].abs() > 2])} (highly skewed)")

print("\nKurtosis Distribution (Normal ≈ 0):")
print(f"  Products with |kurtosis| < 1: {len(stats_df[stats_df['kurtosis'].abs() < 1])} (normal)")
print(f"  Products with |kurtosis| 1-3: {len(stats_df[(stats_df['kurtosis'].abs() >= 1) & (stats_df['kurtosis'].abs() <= 3)])} (slightly heavy tails)")
print(f"  Products with |kurtosis| > 3: {len(stats_df[stats_df['kurtosis'].abs() > 3])} (heavy tails)")

print("\n" + "="*100)
print("✓ ANALYSIS COMPLETE")
print("="*100)
