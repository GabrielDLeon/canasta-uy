"""
Shows rice price distribution using price_min and price_max columns
(the same columns used in the yearly evolution chart).
This should show the extreme values like $359 and $228.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

print("Creating rice min/max price distribution visualizations...")

# Load data
v3_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v3.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v3_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Get rice data
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()

# Add year and merge with product info
rice_prices['year'] = rice_prices['date'].dt.year
rice_prices = rice_prices.merge(
    products_df[['product_id', 'brand', 'name']],
    on='product_id',
    how='left'
)

print(f"Loaded {len(rice_prices)} rice records")

# Filter for 2016 and 2025
rice_2016 = rice_prices[rice_prices['year'] == 2016].copy()
rice_2025 = rice_prices[rice_prices['year'] == 2025].copy()

print(f"2016 records: {len(rice_2016)}")
print(f"2025 records: {len(rice_2025)}")

# Create figure with 6 subplots (3 metrics x 2 years)
fig, axes = plt.subplots(2, 3, figsize=(18, 10))

# ============================================================================
# 2016 - price_min
# ============================================================================
ax1 = axes[0, 0]
brand_order_2016 = rice_2016.groupby('brand')['price_min'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2016, y='brand', x='price_min', order=brand_order_2016, ax=ax1, palette='Blues')
ax1.set_xlabel('price_min ($UY)', fontsize=10, fontweight='bold')
ax1.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax1.set_title('2016 - price_min Distribution', fontsize=11, fontweight='bold')
ax1.grid(True, alpha=0.3, axis='x')

# ============================================================================
# 2016 - price_max
# ============================================================================
ax2 = axes[0, 1]
brand_order_2016_max = rice_2016.groupby('brand')['price_max'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2016, y='brand', x='price_max', order=brand_order_2016_max, ax=ax2, palette='Reds')
ax2.set_xlabel('price_max ($UY)', fontsize=10, fontweight='bold')
ax2.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax2.set_title('2016 - price_max Distribution (SHOWS EXTREMES)', fontsize=11, fontweight='bold', color='red')
ax2.grid(True, alpha=0.3, axis='x')

# Highlight the extreme value
ax2.axvline(x=359, color='red', linestyle='--', linewidth=2, label='Extreme: $359')
ax2.legend()

# ============================================================================
# 2016 - price_avg (for comparison)
# ============================================================================
ax3 = axes[0, 2]
brand_order_2016_avg = rice_2016.groupby('brand')['price_avg'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2016, y='brand', x='price_avg', order=brand_order_2016_avg, ax=ax3, palette='Greens')
ax3.set_xlabel('price_avg ($UY)', fontsize=10, fontweight='bold')
ax3.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax3.set_title('2016 - price_avg Distribution (NORMAL)', fontsize=11, fontweight='bold')
ax3.grid(True, alpha=0.3, axis='x')

# ============================================================================
# 2025 - price_min
# ============================================================================
ax4 = axes[1, 0]
brand_order_2025 = rice_2025.groupby('brand')['price_min'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2025, y='brand', x='price_min', order=brand_order_2025, ax=ax4, palette='Blues')
ax4.set_xlabel('price_min ($UY)', fontsize=10, fontweight='bold')
ax4.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax4.set_title('2025 - price_min Distribution', fontsize=11, fontweight='bold')
ax4.grid(True, alpha=0.3, axis='x')

# ============================================================================
# 2025 - price_max
# ============================================================================
ax5 = axes[1, 1]
brand_order_2025_max = rice_2025.groupby('brand')['price_max'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2025, y='brand', x='price_max', order=brand_order_2025_max, ax=ax5, palette='Reds')
ax5.set_xlabel('price_max ($UY)', fontsize=10, fontweight='bold')
ax5.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax5.set_title('2025 - price_max Distribution (SHOWS EXTREMES)', fontsize=11, fontweight='bold', color='red')
ax5.grid(True, alpha=0.3, axis='x')

# Highlight the extreme value
ax5.axvline(x=228, color='red', linestyle='--', linewidth=2, label='Extreme: $228')
ax5.legend()

# ============================================================================
# 2025 - price_avg (for comparison)
# ============================================================================
ax6 = axes[1, 2]
brand_order_2025_avg = rice_2025.groupby('brand')['price_avg'].median().sort_values(ascending=False).index
sns.boxplot(data=rice_2025, y='brand', x='price_avg', order=brand_order_2025_avg, ax=ax6, palette='Greens')
ax6.set_xlabel('price_avg ($UY)', fontsize=10, fontweight='bold')
ax6.set_ylabel('Brand', fontsize=10, fontweight='bold')
ax6.set_title('2025 - price_avg Distribution (NORMAL)', fontsize=11, fontweight='bold')
ax6.grid(True, alpha=0.3, axis='x')

plt.tight_layout()

# Save
output_path = Path(__file__).parent.parent / "outputs" / "rice_min_max_comparison.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"✓ Chart saved: {output_path}")

plt.show()

# ============================================================================
# Summary
# ============================================================================
print("\n" + "="*80)
print("DETAILED COMPARISON: price_min vs price_max vs price_avg")
print("="*80)

print("\n2016 STATISTICS:")
print("-" * 80)
print(f"price_min: ${rice_2016['price_min'].min():.2f} - ${rice_2016['price_min'].max():.2f}")
print(f"price_max: ${rice_2016['price_max'].min():.2f} - ${rice_2016['price_max'].max():.2f}")
print(f"price_avg: ${rice_2016['price_avg'].min():.2f} - ${rice_2016['price_avg'].max():.2f}")

print("\n2025 STATISTICS:")
print("-" * 80)
print(f"price_min: ${rice_2025['price_min'].min():.2f} - ${rice_2025['price_min'].max():.2f}")
print(f"price_max: ${rice_2025['price_max'].min():.2f} - ${rice_2025['price_max'].max():.2f}")
print(f"price_avg: ${rice_2025['price_avg'].min():.2f} - ${rice_2025['price_avg'].max():.2f}")

print("\n" + "="*80)
print("EXTREME VALUE ANALYSIS")
print("="*80)

# Find records with $359
print("\nRecords with price_max = $359 (2016):")
extreme_359 = rice_2016[rice_2016['price_max'] >= 350]
if len(extreme_359) > 0:
    for idx, row in extreme_359.iterrows():
        print(f"  Brand: {row['brand']}")
        print(f"  Date: {row['date'].date()}")
        print(f"  price_min: ${row['price_min']:.2f}")
        print(f"  price_max: ${row['price_max']:.2f}")
        print(f"  price_avg: ${row['price_avg']:.2f}")
        print()

# Find records with $228
print("Records with price_max >= $220 (2025):")
extreme_228 = rice_2025[rice_2025['price_max'] >= 220]
if len(extreme_228) > 0:
    for idx, row in extreme_228.iterrows():
        print(f"  Brand: {row['brand']}")
        print(f"  Date: {row['date'].date()}")
        print(f"  price_min: ${row['price_min']:.2f}")
        print(f"  price_max: ${row['price_max']:.2f}")
        print(f"  price_avg: ${row['price_avg']:.2f}")
        print()

print("="*80)
