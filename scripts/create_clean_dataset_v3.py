"""
Creates prices_aggregated_all_years_v3.csv

Removes all detected outliers from v2 dataset.
Keeps only clean, reliable price records.

Output: Same structure as v2 but with outlier=1 records excluded
"""

import pandas as pd
from pathlib import Path

print("="*80)
print("CREATING CLEAN DATASET V3 (OUTLIERS EXCLUDED)")
print("="*80)

# Load v2 data
v2_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v2.csv"

print("\n1. Loading v2 dataset...")
prices_v2 = pd.read_csv(v2_path)
print(f"  - Total records in v2: {len(prices_v2):,}")
print(f"  - Columns: {list(prices_v2.columns)}")

# Convert date to datetime
prices_v2['date'] = pd.to_datetime(prices_v2['date'])

# Count outliers
outlier_count = prices_v2['outlier'].sum()
print(f"\n2. Outlier Summary")
print(f"  - Total outliers in v2: {outlier_count:,}")
print(f"  - Outlier percentage: {outlier_count/len(prices_v2)*100:.2f}%")

# Create v3 by filtering out outliers
print(f"\n3. Creating v3 (excluding outliers)...")
prices_v3 = prices_v2[prices_v2['outlier'] == 0].copy()

clean_count = len(prices_v3)
removed_count = len(prices_v2) - clean_count

print(f"  - Records after removing outliers: {clean_count:,}")
print(f"  - Records removed: {removed_count:,}")
print(f"  - Retention rate: {clean_count/len(prices_v2)*100:.2f}%")

# Drop the outlier column (no longer needed)
prices_v3 = prices_v3.drop('outlier', axis=1)

print(f"\n4. Final v3 Structure")
print(f"  - Columns: {list(prices_v3.columns)}")
print(f"  - Shape: {prices_v3.shape[0]:,} rows × {prices_v3.shape[1]} columns")

# Save to CSV
output_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v3.csv"

print(f"\n5. Saving v3 dataset...")
prices_v3.to_csv(output_path, index=False)
print(f"  ✓ Saved to: {output_path}")

# Generate summary by category
print(f"\n6. Outlier Removal by Category")
print("="*80)

outlier_summary = []
for category in prices_v2['category'].unique():
    cat_v2 = prices_v2[prices_v2['category'] == category]
    cat_v3 = prices_v3[prices_v3['category'] == category]

    cat_outliers = cat_v2['outlier'].sum()
    cat_removal_pct = (cat_outliers / len(cat_v2) * 100) if len(cat_v2) > 0 else 0

    outlier_summary.append({
        'category': category,
        'v2_count': len(cat_v2),
        'v3_count': len(cat_v3),
        'outliers': cat_outliers,
        'removal_pct': cat_removal_pct
    })

outlier_summary_df = pd.DataFrame(outlier_summary).sort_values('removal_pct', ascending=False)

print("\nTop 20 categories by outlier removal:")
for idx, row in outlier_summary_df.head(20).iterrows():
    print(f"  {row['category']:50s} | {row['removal_pct']:5.2f}% removed | {row['v2_count']:5d} → {row['v3_count']:5d}")

# Data quality improvements
print(f"\n7. Data Quality Improvements (V2 → V3)")
print("="*80)

print("\nPrice Statistics Comparison:")
print(f"\nV2 (with outliers):")
print(f"  Min price: ${prices_v2['price_avg'].min():.2f}")
print(f"  Max price: ${prices_v2['price_avg'].max():.2f}")
print(f"  Mean price: ${prices_v2['price_avg'].mean():.2f}")
print(f"  Median price: ${prices_v2['price_avg'].median():.2f}")
print(f"  Std deviation: ${prices_v2['price_avg'].std():.2f}")

print(f"\nV3 (outliers removed):")
print(f"  Min price: ${prices_v3['price_avg'].min():.2f}")
print(f"  Max price: ${prices_v3['price_avg'].max():.2f}")
print(f"  Mean price: ${prices_v3['price_avg'].mean():.2f}")
print(f"  Median price: ${prices_v3['price_avg'].median():.2f}")
print(f"  Std deviation: ${prices_v3['price_avg'].std():.2f}")

# Calculate improvement
mean_reduction = ((prices_v2['price_avg'].mean() - prices_v3['price_avg'].mean()) /
                  prices_v2['price_avg'].mean() * 100)
std_reduction = ((prices_v2['price_avg'].std() - prices_v3['price_avg'].std()) /
                 prices_v2['price_avg'].std() * 100)

print(f"\nImprovement:")
print(f"  Mean price reduction: {mean_reduction:.2f}%")
print(f"  Std deviation reduction: {std_reduction:.2f}%")

# Summary by year
print(f"\n8. Records by Year (V2 → V3)")
print("="*80)

prices_v2['year'] = prices_v2['date'].dt.year
prices_v3['year'] = prices_v3['date'].dt.year

for year in sorted(prices_v2['year'].unique()):
    v2_year = len(prices_v2[prices_v2['year'] == year])
    v3_year = len(prices_v3[prices_v3['year'] == year])
    removed = v2_year - v3_year
    removal_pct = (removed / v2_year * 100) if v2_year > 0 else 0

    print(f"  {year}: {v2_year:6d} → {v3_year:6d} | Removed: {removed:5d} ({removal_pct:5.2f}%)")

print(f"\n" + "="*80)
print("✓ V3 CLEAN DATASET CREATED SUCCESSFULLY")
print("="*80)
print(f"\nYou can now use: prices_aggregated_all_years_v3.csv")
print(f"Size: {output_path.stat().st_size / 1024 / 1024:.1f} MB")
