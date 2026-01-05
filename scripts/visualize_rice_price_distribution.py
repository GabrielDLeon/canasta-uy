"""
Visualizes rice price distribution by brand for 2016 vs 2025.
Shows how each rice brand is positioned in the market across the price spectrum.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path

print("Creating rice price distribution visualizations...")

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

# Create figure with subplots
fig, axes = plt.subplots(2, 2, figsize=(16, 12))

# ============================================================================
# Plot 1: Box plot by brand - 2016
# ============================================================================
ax1 = axes[0, 0]

# Order brands by median price
rice_2016_sorted = rice_2016.sort_values('price_avg', ascending=False)
brand_order_2016 = rice_2016_sorted.groupby('brand')['price_avg'].median().sort_values(ascending=False).index

sns.boxplot(data=rice_2016, y='brand', x='price_avg', order=brand_order_2016, ax=ax1, palette='Set2')
ax1.set_xlabel('Price ($UY)', fontsize=11, fontweight='bold')
ax1.set_ylabel('Brand', fontsize=11, fontweight='bold')
ax1.set_title('Rice Price Distribution by Brand - 2016', fontsize=12, fontweight='bold')
ax1.grid(True, alpha=0.3, axis='x')

# Add mean values as text
for i, brand in enumerate(brand_order_2016):
    brand_data = rice_2016[rice_2016['brand'] == brand]['price_avg']
    mean_val = brand_data.mean()
    ax1.text(mean_val, i, f' ${mean_val:.2f}', va='center', fontsize=9, fontweight='bold')

# ============================================================================
# Plot 2: Box plot by brand - 2025
# ============================================================================
ax2 = axes[0, 1]

# Order brands by median price
rice_2025_sorted = rice_2025.sort_values('price_avg', ascending=False)
brand_order_2025 = rice_2025_sorted.groupby('brand')['price_avg'].median().sort_values(ascending=False).index

sns.boxplot(data=rice_2025, y='brand', x='price_avg', order=brand_order_2025, ax=ax2, palette='Set3')
ax2.set_xlabel('Price ($UY)', fontsize=11, fontweight='bold')
ax2.set_ylabel('Brand', fontsize=11, fontweight='bold')
ax2.set_title('Rice Price Distribution by Brand - 2025', fontsize=12, fontweight='bold')
ax2.grid(True, alpha=0.3, axis='x')

# Add mean values as text
for i, brand in enumerate(brand_order_2025):
    brand_data = rice_2025[rice_2025['brand'] == brand]['price_avg']
    mean_val = brand_data.mean()
    ax2.text(mean_val, i, f' ${mean_val:.2f}', va='center', fontsize=9, fontweight='bold')

# ============================================================================
# Plot 3: Violin plot - 2016
# ============================================================================
ax3 = axes[1, 0]

sns.violinplot(data=rice_2016, y='brand', x='price_avg', order=brand_order_2016, ax=ax3, palette='Set2')
ax3.set_xlabel('Price ($UY)', fontsize=11, fontweight='bold')
ax3.set_ylabel('Brand', fontsize=11, fontweight='bold')
ax3.set_title('Rice Price Distribution (Density) - 2016', fontsize=12, fontweight='bold')
ax3.grid(True, alpha=0.3, axis='x')

# ============================================================================
# Plot 4: Violin plot - 2025
# ============================================================================
ax4 = axes[1, 1]

sns.violinplot(data=rice_2025, y='brand', x='price_avg', order=brand_order_2025, ax=ax4, palette='Set3')
ax4.set_xlabel('Price ($UY)', fontsize=11, fontweight='bold')
ax4.set_ylabel('Brand', fontsize=11, fontweight='bold')
ax4.set_title('Rice Price Distribution (Density) - 2025', fontsize=12, fontweight='bold')
ax4.grid(True, alpha=0.3, axis='x')

plt.tight_layout()

# Save
output_path = Path(__file__).parent.parent / "outputs" / "rice_price_distribution_comparison.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"✓ Chart saved: {output_path}")

plt.show()

# ============================================================================
# Generate Summary Statistics
# ============================================================================
print("\n" + "="*80)
print("RICE BRAND PRICE STATISTICS")
print("="*80)

print("\n2016 - BY BRAND:")
print("-" * 80)
for brand in brand_order_2016:
    data = rice_2016[rice_2016['brand'] == brand]['price_avg']
    print(f"\n{brand}:")
    print(f"  Count: {len(data):4d} records")
    print(f"  Min:   ${data.min():7.2f}")
    print(f"  Max:   ${data.max():7.2f}")
    print(f"  Mean:  ${data.mean():7.2f}")
    print(f"  Median:${data.median():7.2f}")
    print(f"  Std:   ${data.std():7.2f}")

print("\n2025 - BY BRAND:")
print("-" * 80)
for brand in brand_order_2025:
    data = rice_2025[rice_2025['brand'] == brand]['price_avg']
    print(f"\n{brand}:")
    print(f"  Count: {len(data):4d} records")
    print(f"  Min:   ${data.min():7.2f}")
    print(f"  Max:   ${data.max():7.2f}")
    print(f"  Mean:  ${data.mean():7.2f}")
    print(f"  Median:${data.median():7.2f}")
    print(f"  Std:   ${data.std():7.2f}")

# ============================================================================
# Brand Positioning Comparison
# ============================================================================
print("\n" + "="*80)
print("BRAND POSITIONING: 2016 vs 2025")
print("="*80)

print("\nPrice change by brand (2016 → 2025):")
print("-" * 80)

all_brands = set(rice_2016['brand'].unique()) | set(rice_2025['brand'].unique())

for brand in sorted(all_brands):
    mean_2016 = rice_2016[rice_2016['brand'] == brand]['price_avg'].mean()
    mean_2025 = rice_2025[rice_2025['brand'] == brand]['price_avg'].mean()

    if pd.notna(mean_2016) and pd.notna(mean_2025):
        change = mean_2025 - mean_2016
        pct_change = (change / mean_2016) * 100
        print(f"{brand:30s}: ${mean_2016:6.2f} → ${mean_2025:6.2f} | Change: {change:+6.2f} ({pct_change:+6.1f}%)")
    elif pd.notna(mean_2016):
        print(f"{brand:30s}: ${mean_2016:6.2f} → N/A (not available in 2025)")
    else:
        print(f"{brand:30s}: N/A (not available in 2016) → ${mean_2025:6.2f}")

print("\n" + "="*80)
print("✓ ANALYSIS COMPLETE")
print("="*80)
