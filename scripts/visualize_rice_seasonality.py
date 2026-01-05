"""
Visualización de estacionalidad en precios de arroz.
Muestra patrones de precios a lo largo del año.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path

# Load data
v4_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Get rice products
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()

# Filter rice prices
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()
rice_prices['month'] = rice_prices['date'].dt.month
rice_prices['year'] = rice_prices['date'].dt.year

# Create figure with 3 subplots
fig = plt.figure(figsize=(18, 12))
gs = fig.add_gridspec(3, 2, hspace=0.3, wspace=0.3)

month_names = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
month_full = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
              'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

# ============================================================================
# Plot 1: Average price by month
# ============================================================================
ax1 = fig.add_subplot(gs[0, :])

monthly_avg = rice_prices.groupby('month')['price_avg'].mean()
monthly_median = rice_prices.groupby('month')['price_avg'].median()
monthly_std = rice_prices.groupby('month')['price_avg'].std()

ax1.plot(monthly_avg.index, monthly_avg.values, marker='o', linewidth=3, markersize=10,
         color='darkblue', label='Promedio', zorder=3)
ax1.plot(monthly_median.index, monthly_median.values, marker='s', linewidth=2, markersize=8,
         color='orange', linestyle='--', label='Mediana', zorder=2)

# Shade the area between min and max
monthly_min = rice_prices.groupby('month')['price_avg'].min()
monthly_max = rice_prices.groupby('month')['price_avg'].max()
ax1.fill_between(monthly_avg.index, monthly_min.values, monthly_max.values,
                 alpha=0.2, color='steelblue', label='Rango (Min-Max)')

# Highlight cheapest and most expensive months
cheapest_month = monthly_avg.idxmin()
most_expensive_month = monthly_avg.idxmax()

ax1.axvline(cheapest_month, color='green', linestyle=':', linewidth=2, alpha=0.7)
ax1.axvline(most_expensive_month, color='red', linestyle=':', linewidth=2, alpha=0.7)

ax1.scatter(cheapest_month, monthly_avg[cheapest_month], s=300, color='green', marker='*', zorder=4, label='Mes más barato')
ax1.scatter(most_expensive_month, monthly_avg[most_expensive_month], s=300, color='red', marker='*', zorder=4, label='Mes más caro')

ax1.set_xlabel('Mes', fontsize=12, fontweight='bold')
ax1.set_ylabel('Precio ($UY)', fontsize=12, fontweight='bold')
ax1.set_title('Estacionalidad de Precios de Arroz: Promedio por Mes (2016-2025)',
              fontsize=13, fontweight='bold', pad=15)
ax1.set_xticks(range(1, 13))
ax1.set_xticklabels(month_names)
ax1.grid(True, alpha=0.3)
ax1.legend(fontsize=10, loc='upper left', ncol=3)

# Add value annotations
for month in range(1, 13):
    ax1.text(month, monthly_avg[month] + 0.3, f'${monthly_avg[month]:.2f}',
            ha='center', fontsize=8, fontweight='bold')

# ============================================================================
# Plot 2: Heatmap of prices by month and year
# ============================================================================
ax2 = fig.add_subplot(gs[1, :])

pivot_data = rice_prices.pivot_table(values='price_avg', index='year', columns='month', aggfunc='mean')
sns.heatmap(pivot_data, annot=True, fmt='.1f', cmap='RdYlGn_r', ax=ax2,
            cbar_kws={'label': 'Precio ($UY)'}, linewidths=0.5)
ax2.set_xlabel('Mes', fontsize=12, fontweight='bold')
ax2.set_ylabel('Año', fontsize=12, fontweight='bold')
ax2.set_title('Mapa de Calor: Precios de Arroz por Mes y Año',
              fontsize=13, fontweight='bold', pad=15)
ax2.set_xticklabels(month_names)

# ============================================================================
# Plot 3: Box plot by month
# ============================================================================
ax3 = fig.add_subplot(gs[2, 0])

box_data = [rice_prices[rice_prices['month'] == m]['price_avg'].values for m in range(1, 13)]
bp = ax3.boxplot(box_data, labels=month_names, patch_artist=True)

# Color boxes based on median price
medians = [np.median(d) for d in box_data]
min_median = min(medians)
max_median = max(medians)

for patch, median in zip(bp['boxes'], medians):
    color_intensity = (median - min_median) / (max_median - min_median)
    patch.set_facecolor(plt.cm.RdYlGn_r(color_intensity))
    patch.set_alpha(0.7)

ax3.set_xlabel('Mes', fontsize=11, fontweight='bold')
ax3.set_ylabel('Precio ($UY)', fontsize=11, fontweight='bold')
ax3.set_title('Distribución de Precios por Mes', fontsize=12, fontweight='bold')
ax3.grid(True, alpha=0.3, axis='y')

# ============================================================================
# Plot 4: Quarterly comparison
# ============================================================================
ax4 = fig.add_subplot(gs[2, 1])

rice_prices['quarter'] = rice_prices['date'].dt.quarter
quarterly = rice_prices.groupby('quarter')['price_avg'].agg(['mean', 'std']).reset_index()

quarters = ['Q1\n(Ene-Mar)', 'Q2\n(Abr-Jun)', 'Q3\n(Jul-Sep)', 'Q4\n(Oct-Dic)']
colors_quarter = ['green', 'yellow', 'red', 'orange']

bars = ax4.bar(quarterly['quarter'], quarterly['mean'], yerr=quarterly['std'],
               color=colors_quarter, alpha=0.7, edgecolor='black', capsize=5)

ax4.set_ylabel('Precio Promedio ($UY)', fontsize=11, fontweight='bold')
ax4.set_title('Comparación por Trimestre', fontsize=12, fontweight='bold')
ax4.set_xticks(quarterly['quarter'])
ax4.set_xticklabels(quarters)
ax4.grid(True, alpha=0.3, axis='y')

# Add value labels
for i, (q, mean, std) in enumerate(zip(quarterly['quarter'], quarterly['mean'], quarterly['std'])):
    ax4.text(q, mean + std + 0.2, f'${mean:.2f}', ha='center', fontsize=10, fontweight='bold')

plt.suptitle('Análisis de Estacionalidad: Arroz Blanco en Uruguay (2016-2025)',
             fontsize=15, fontweight='bold', y=0.995)

# Save
output_path = Path(__file__).parent.parent / "outputs" / "rice_seasonality_analysis.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"✓ Chart saved: {output_path}")

plt.show()
