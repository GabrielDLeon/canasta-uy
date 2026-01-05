"""
Visualización de resistencia a la inflación por marca de arroz.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Load data
v4_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Get rice products
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()
rice_products = products_df[products_df['category'] == 'Arroz blanco'].set_index('product_id')

# Filter rice prices
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()
rice_prices['year'] = rice_prices['date'].dt.year

# Get 2016 and 2025 data
rice_2016 = rice_prices[rice_prices['year'] == 2016]
rice_2025 = rice_prices[rice_prices['year'] == 2025]

# Analyze by brand
brands_data = []
for product_id in rice_product_ids:
    brand = rice_products.loc[product_id, 'brand']
    data_2016 = rice_2016[rice_2016['product_id'] == product_id]['price_avg']
    data_2025 = rice_2025[rice_2025['product_id'] == product_id]['price_avg']

    if len(data_2016) > 0 and len(data_2025) > 0:
        price_2016 = data_2016.mean()
        price_2025 = data_2025.mean()
        percent_change = ((price_2025 - price_2016) / price_2016) * 100

        brands_data.append({
            'brand': brand,
            'price_2016': price_2016,
            'price_2025': price_2025,
            'percent_change': percent_change
        })

brands_df = pd.DataFrame(brands_data).sort_values('percent_change')

# Create figure with 2 subplots
fig, axes = plt.subplots(1, 2, figsize=(16, 8))

# Plot 1: Inflation percentage by brand (horizontal bar chart)
ax1 = axes[0]
colors = ['#2ecc71' if x < 50 else '#f39c12' if x < 70 else '#e74c3c'
          for x in brands_df['percent_change']]
ax1.barh(range(len(brands_df)), brands_df['percent_change'], color=colors, edgecolor='black', alpha=0.8)
ax1.set_yticks(range(len(brands_df)))
ax1.set_yticklabels(brands_df['brand'], fontsize=11, fontweight='bold')
ax1.set_xlabel('Aumento de Precio (%)', fontsize=12, fontweight='bold')
ax1.set_title('Resistencia a la Inflación por Marca\nArroz Blanco 2016 → 2025',
              fontsize=13, fontweight='bold', pad=20)
ax1.grid(True, alpha=0.3, axis='x')
ax1.invert_yaxis()

# Add value labels
for i, (idx, row) in enumerate(brands_df.iterrows()):
    ax1.text(row['percent_change'] + 1, i, f"{row['percent_change']:.1f}%",
             va='center', fontsize=10, fontweight='bold')

# Add average line
avg_inflation = brands_df['percent_change'].mean()
ax1.axvline(avg_inflation, color='red', linestyle='--', linewidth=2, alpha=0.7, label=f'Promedio: {avg_inflation:.1f}%')
ax1.legend(fontsize=10, loc='lower right')

# Plot 2: Price comparison 2016 vs 2025
ax2 = axes[1]
x = range(len(brands_df))
width = 0.35

bars1 = ax2.bar([i - width/2 for i in x], brands_df['price_2016'], width,
                label='2016', color='steelblue', edgecolor='black', alpha=0.8)
bars2 = ax2.bar([i + width/2 for i in x], brands_df['price_2025'], width,
                label='2025', color='coral', edgecolor='black', alpha=0.8)

ax2.set_xlabel('Marca', fontsize=12, fontweight='bold')
ax2.set_ylabel('Precio Promedio ($UY)', fontsize=12, fontweight='bold')
ax2.set_title('Comparación de Precios: 2016 vs 2025', fontsize=13, fontweight='bold', pad=20)
ax2.set_xticks(x)
ax2.set_xticklabels(brands_df['brand'], rotation=45, ha='right', fontsize=10)
ax2.legend(fontsize=11, loc='upper left')
ax2.grid(True, alpha=0.3, axis='y')

# Add value labels on bars
for bar in bars1:
    height = bar.get_height()
    ax2.text(bar.get_x() + bar.get_width()/2., height,
            f'${height:.0f}', ha='center', va='bottom', fontsize=8)

for bar in bars2:
    height = bar.get_height()
    ax2.text(bar.get_x() + bar.get_width()/2., height,
            f'${height:.0f}', ha='center', va='bottom', fontsize=8)

plt.tight_layout()

# Save
output_path = Path(__file__).parent.parent / "outputs" / "brand_inflation_comparison.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"✓ Chart saved: {output_path}")

plt.show()
