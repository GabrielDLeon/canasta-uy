"""
Investigates which rice products are causing the extreme max/min prices in 2016 and 2025.
"""

import pandas as pd
from pathlib import Path

print("="*80)
print("INVESTIGATING RICE PRICE EXTREMES (V3)")
print("="*80)

# Load data
v3_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v3.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v3_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Filter rice products
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()
rice_prices['year'] = rice_prices['date'].dt.year

print("\n" + "="*80)
print("EXTREMES IN 2016 (Rango: $6.6-$359.0)")
print("="*80)

rice_2016 = rice_prices[rice_prices['year'] == 2016]
max_2016 = rice_2016[rice_2016['price_avg'] == rice_2016['price_avg'].max()].iloc[0]
min_2016 = rice_2016[rice_2016['price_avg'] == rice_2016['price_avg'].min()].iloc[0]

print(f"\nMÁXIMO en 2016:")
prod_max = products_df[products_df['product_id'] == max_2016['product_id']].iloc[0]
print(f"  Producto ID {max_2016['product_id']}: {prod_max['name']}")
print(f"  Precio: ${max_2016['price_avg']:.2f}")
print(f"  Fecha: {max_2016['date'].date()}")

print(f"\nMÍNIMO en 2016:")
prod_min = products_df[products_df['product_id'] == min_2016['product_id']].iloc[0]
print(f"  Producto ID {min_2016['product_id']}: {prod_min['name']}")
print(f"  Precio: ${min_2016['price_avg']:.2f}")
print(f"  Fecha: {min_2016['date'].date()}")

# Analyze all 2016 prices by product
print(f"\n2016 - Precios por producto:")
for product_id in sorted(rice_product_ids):
    prod = products_df[products_df['product_id'] == product_id].iloc[0]
    prod_2016 = rice_2016[rice_2016['product_id'] == product_id]

    if len(prod_2016) > 0:
        print(f"\n  {prod['name']}:")
        print(f"    Registros: {len(prod_2016)}")
        print(f"    Min: ${prod_2016['price_avg'].min():.2f}")
        print(f"    Max: ${prod_2016['price_avg'].max():.2f}")
        print(f"    Media: ${prod_2016['price_avg'].mean():.2f}")
        print(f"    Mediana: ${prod_2016['price_avg'].median():.2f}")
        print(f"    Std: ${prod_2016['price_avg'].std():.2f}")

print("\n" + "="*80)
print("EXTREMES IN 2025 (Rango: $16.9-$228.0)")
print("="*80)

rice_2025 = rice_prices[rice_prices['year'] == 2025]
max_2025 = rice_2025[rice_2025['price_avg'] == rice_2025['price_avg'].max()].iloc[0]
min_2025 = rice_2025[rice_2025['price_avg'] == rice_2025['price_avg'].min()].iloc[0]

print(f"\nMÁXIMO en 2025:")
prod_max = products_df[products_df['product_id'] == max_2025['product_id']].iloc[0]
print(f"  Producto ID {max_2025['product_id']}: {prod_max['name']}")
print(f"  Precio: ${max_2025['price_avg']:.2f}")
print(f"  Fecha: {max_2025['date'].date()}")

print(f"\nMÍNIMO en 2025:")
prod_min = products_df[products_df['product_id'] == min_2025['product_id']].iloc[0]
print(f"  Producto ID {min_2025['product_id']}: {prod_min['name']}")
print(f"  Precio: ${min_2025['price_avg']:.2f}")
print(f"  Fecha: {min_2025['date'].date()}")

# Analyze all 2025 prices by product
print(f"\n2025 - Precios por producto:")
for product_id in sorted(rice_product_ids):
    prod = products_df[products_df['product_id'] == product_id].iloc[0]
    prod_2025 = rice_2025[rice_2025['product_id'] == product_id]

    if len(prod_2025) > 0:
        print(f"\n  {prod['name']}:")
        print(f"    Registros: {len(prod_2025)}")
        print(f"    Min: ${prod_2025['price_avg'].min():.2f}")
        print(f"    Max: ${prod_2025['price_avg'].max():.2f}")
        print(f"    Media: ${prod_2025['price_avg'].mean():.2f}")
        print(f"    Mediana: ${prod_2025['price_avg'].median():.2f}")
        print(f"    Std: ${prod_2025['price_avg'].std():.2f}")

# Compare years
print("\n" + "="*80)
print("COMPARISON: Are these products consistently at these price levels?")
print("="*80)

problem_product_2016 = max_2016['product_id']
problem_product_2025 = max_2025['product_id']

print(f"\nProducto con máximo en 2016 (ID {problem_product_2016}):")
all_years_prod_2016 = rice_prices[rice_prices['product_id'] == problem_product_2016]
for year in sorted(all_years_prod_2016['year'].unique()):
    year_data = all_years_prod_2016[all_years_prod_2016['year'] == year]
    if len(year_data) > 0:
        print(f"  {year}: ${year_data['price_avg'].mean():.2f} (rango: ${year_data['price_avg'].min():.2f}-${year_data['price_avg'].max():.2f})")

print(f"\nProducto con máximo en 2025 (ID {problem_product_2025}):")
all_years_prod_2025 = rice_prices[rice_prices['product_id'] == problem_product_2025]
for year in sorted(all_years_prod_2025['year'].unique()):
    year_data = all_years_prod_2025[all_years_prod_2025['year'] == year]
    if len(year_data) > 0:
        print(f"  {year}: ${year_data['price_avg'].mean():.2f} (rango: ${year_data['price_avg'].min():.2f}-${year_data['price_avg'].max():.2f})")

print("\n" + "="*80)
