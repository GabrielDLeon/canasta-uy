"""
Comprehensive outlier analysis script.

Analyzes detected outliers to understand:
1. Which products are most frequently outliers
2. Price ratios vs group median
3. Temporal patterns
4. Category-specific insights
5. Statistical distribution
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

print("="*80)
print("OUTLIER ANALYSIS SCRIPT")
print("="*80)

# Load data
print("\n1. Loading data...")
v2_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v2.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v2_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Filter outliers
outliers = prices_df[prices_df['outlier'] == 1].copy()
normal = prices_df[prices_df['outlier'] == 0].copy()

print(f"  - Total records: {len(prices_df):,}")
print(f"  - Outliers: {len(outliers):,} ({len(outliers)/len(prices_df)*100:.2f}%)")
print(f"  - Normal: {len(normal):,} ({len(normal)/len(prices_df)*100:.2f}%)")

# 1. BASIC OUTLIER STATISTICS
print("\n" + "="*80)
print("2. BASIC OUTLIER STATISTICS")
print("="*80)

print("\nOutlier Price Distribution:")
print(f"  Min price (outlier): ${outliers['price_avg'].min():.2f}")
print(f"  Max price (outlier): ${outliers['price_avg'].max():.2f}")
print(f"  Mean price (outlier): ${outliers['price_avg'].mean():.2f}")
print(f"  Median price (outlier): ${outliers['price_avg'].median():.2f}")

print("\nNormal Price Distribution:")
print(f"  Min price (normal): ${normal['price_avg'].min():.2f}")
print(f"  Max price (normal): ${normal['price_avg'].max():.2f}")
print(f"  Mean price (normal): ${normal['price_avg'].mean():.2f}")
print(f"  Median price (normal): ${normal['price_avg'].median():.2f}")

# 2. PRODUCTS THAT ARE MOST FREQUENTLY OUTLIERS
print("\n" + "="*80)
print("3. TOP 15 PRODUCTS THAT ARE MOST FREQUENTLY OUTLIERS")
print("="*80)

product_outlier_freq = outliers.groupby('product_id').size().sort_values(ascending=False)
for i, (product_id, count) in enumerate(product_outlier_freq.head(15).items(), 1):
    # Get product info
    prod = products_df[products_df['product_id'] == product_id].iloc[0]
    total_records = len(prices_df[prices_df['product_id'] == product_id])
    pct = (count / total_records) * 100
    print(f"{i:2d}. ID {product_id:3d} | {pct:5.1f}% outlier | {count:5d}/{total_records:5d} | {prod['name'][:50]}")

# 3. CATEGORY STATISTICS
print("\n" + "="*80)
print("4. OUTLIER PERCENTAGE BY CATEGORY (Top 15)")
print("="*80)

category_stats = []
for category in prices_df['category'].unique():
    cat_data = prices_df[prices_df['category'] == category]
    cat_outliers = outliers[outliers['category'] == category]

    pct = (len(cat_outliers) / len(cat_data)) * 100
    category_stats.append({
        'category': category,
        'total': len(cat_data),
        'outliers': len(cat_outliers),
        'pct': pct
    })

category_stats_df = pd.DataFrame(category_stats).sort_values('pct', ascending=False)

for i, row in category_stats_df.head(15).iterrows():
    print(f"{row['category']:50s} | {row['pct']:6.2f}% | {row['outliers']:5d}/{row['total']:5d}")

# 4. PRICE RATIO ANALYSIS
print("\n" + "="*80)
print("5. PRICE RATIO ANALYSIS (Outlier vs Group Median)")
print("="*80)

# Calculate price ratio for each outlier vs group median
price_ratios = []
for (category, year_month), group in prices_df.groupby(['category', 'year_month']):
    group_median = group['price_avg'].median()

    if pd.isna(group_median) or group_median == 0:
        continue

    group_outliers = group[group['outlier'] == 1]
    for idx, row in group_outliers.iterrows():
        ratio = row['price_avg'] / group_median
        price_ratios.append({
            'product_id': row['product_id'],
            'category': category,
            'date': row['date'],
            'price': row['price_avg'],
            'median': group_median,
            'ratio': ratio,
            'higher': ratio > 1
        })

if price_ratios:
    ratio_df = pd.DataFrame(price_ratios)

    print(f"\nPrice Ratio Statistics (outlier/group median):")
    print(f"  Min ratio: {ratio_df['ratio'].min():.2f}x")
    print(f"  Max ratio: {ratio_df['ratio'].max():.2f}x")
    print(f"  Mean ratio: {ratio_df['ratio'].mean():.2f}x")
    print(f"  Median ratio: {ratio_df['ratio'].median():.2f}x")

    print(f"\n  Outliers HIGHER than median: {ratio_df['higher'].sum()} ({ratio_df['higher'].sum()/len(ratio_df)*100:.1f}%)")
    print(f"  Outliers LOWER than median: {(~ratio_df['higher']).sum()} ({(~ratio_df['higher']).sum()/len(ratio_df)*100:.1f}%)")

    print(f"\nPrice Ratio Percentiles:")
    for pct in [10, 25, 50, 75, 90]:
        val = ratio_df['ratio'].quantile(pct/100)
        print(f"  {pct}th percentile: {val:.2f}x")

# 5. TEMPORAL PATTERNS
print("\n" + "="*80)
print("6. TEMPORAL PATTERNS - Outliers by Year")
print("="*80)

outliers['year'] = outliers['date'].dt.year
year_stats = outliers.groupby('year').size()
for year, count in year_stats.items():
    total_in_year = len(prices_df[prices_df['date'].dt.year == year])
    pct = (count / total_in_year) * 100
    print(f"  {year}: {count:5d} outliers ({pct:5.2f}%) out of {total_in_year:,} records")

# 6. SPECIFIC CATEGORY DEEP DIVE
print("\n" + "="*80)
print("7. CATEGORY DEEP DIVE - Afeitadora (highest outlier %)")
print("="*80)

afeitadora = prices_df[prices_df['category'] == 'Afeitadora']
afeitadora_outliers = outliers[outliers['category'] == 'Afeitadora']

print(f"\nTotal Afeitadora records: {len(afeitadora):,}")
print(f"Outliers: {len(afeitadora_outliers):,} ({len(afeitadora_outliers)/len(afeitadora)*100:.1f}%)")

# Get products in Afeitadora
afeitadora_products = products_df[products_df['category'] == 'Afeitadora'][['product_id', 'brand', 'name']]
print(f"Products in category: {len(afeitadora_products)}")

for _, prod in afeitadora_products.iterrows():
    prod_data = afeitadora[afeitadora['product_id'] == prod['product_id']]
    prod_outliers = afeitadora_outliers[afeitadora_outliers['product_id'] == prod['product_id']]

    if len(prod_data) > 0:
        price_mean = prod_data['price_avg'].mean()
        price_std = prod_data['price_avg'].std()
        outlier_pct = (len(prod_outliers) / len(prod_data)) * 100

        print(f"  {prod['brand']:30s} | Avg: ${price_mean:7.2f} | Std: ${price_std:7.2f} | Outliers: {outlier_pct:5.1f}%")

# 7. CREATE VISUALIZATIONS
print("\n" + "="*80)
print("8. GENERATING VISUALIZATIONS")
print("="*80)

fig, axes = plt.subplots(2, 2, figsize=(16, 12))

# Plot 1: Price distribution - Normal vs Outlier
ax1 = axes[0, 0]
ax1.hist(normal['price_avg'], bins=100, alpha=0.6, label='Normal', color='green', edgecolor='black')
ax1.hist(outliers['price_avg'], bins=100, alpha=0.6, label='Outlier', color='red', edgecolor='black')
ax1.set_xlabel('Price ($UY)', fontsize=11, fontweight='bold')
ax1.set_ylabel('Frequency', fontsize=11, fontweight='bold')
ax1.set_title('Price Distribution: Normal vs Outliers', fontsize=12, fontweight='bold')
ax1.legend(fontsize=10)
ax1.grid(True, alpha=0.3)

# Plot 2: Outlier percentage by category (top 15)
ax2 = axes[0, 1]
top_categories = category_stats_df.head(15)
colors = plt.cm.Reds(np.linspace(0.3, 0.9, len(top_categories)))
ax2.barh(range(len(top_categories)), top_categories['pct'], color=colors)
ax2.set_yticks(range(len(top_categories)))
ax2.set_yticklabels(top_categories['category'], fontsize=9)
ax2.set_xlabel('Outlier Percentage (%)', fontsize=11, fontweight='bold')
ax2.set_title('Top 15 Categories by Outlier Percentage', fontsize=12, fontweight='bold')
ax2.grid(True, alpha=0.3, axis='x')
ax2.invert_yaxis()

# Plot 3: Outliers by year
ax3 = axes[1, 0]
outliers['year'] = outliers['date'].dt.year
year_counts = outliers.groupby('year').size()
ax3.bar(year_counts.index, year_counts.values, color='coral', edgecolor='black', alpha=0.7)
ax3.set_xlabel('Year', fontsize=11, fontweight='bold')
ax3.set_ylabel('Number of Outliers', fontsize=11, fontweight='bold')
ax3.set_title('Outliers by Year', fontsize=12, fontweight='bold')
ax3.grid(True, alpha=0.3, axis='y')

# Plot 4: Price ratio distribution (if available)
ax4 = axes[1, 1]
if price_ratios:
    ax4.hist(ratio_df['ratio'], bins=100, color='steelblue', edgecolor='black', alpha=0.7)
    ax4.axvline(ratio_df['ratio'].median(), color='red', linestyle='--', linewidth=2, label=f'Median: {ratio_df["ratio"].median():.2f}x')
    ax4.set_xlabel('Price Ratio (Outlier/Median)', fontsize=11, fontweight='bold')
    ax4.set_ylabel('Frequency', fontsize=11, fontweight='bold')
    ax4.set_title('Distribution of Price Ratios', fontsize=12, fontweight='bold')
    ax4.legend(fontsize=10)
    ax4.grid(True, alpha=0.3)
    # Set x-axis limit for better visibility
    ax4.set_xlim([0, 10])

plt.tight_layout()

output_path = Path(__file__).parent.parent.parent / "outputs" / "outlier_analysis.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"  ✓ Visualization saved: {output_path}")

plt.show()

print("\n" + "="*80)
print("✓ OUTLIER ANALYSIS COMPLETE")
print("="*80)
