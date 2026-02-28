"""
Análisis de resistencia a la inflación por marca de arroz.
Compara precios 2016 vs 2025 para determinar qué marca mantuvo mejor su precio.
"""

import pandas as pd
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

print("="*100)
print("ANÁLISIS DE RESISTENCIA A LA INFLACIÓN: ARROZ BLANCO (2016 vs 2025)")
print("="*100)

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

        absolute_change = price_2025 - price_2016
        percent_change = ((price_2025 - price_2016) / price_2016) * 100

        brands_data.append({
            'brand': brand,
            'product_id': product_id,
            'price_2016': price_2016,
            'price_2025': price_2025,
            'absolute_change': absolute_change,
            'percent_change': percent_change,
            'records_2016': len(data_2016),
            'records_2025': len(data_2025)
        })

brands_df = pd.DataFrame(brands_data).sort_values('percent_change')

print("\n📊 RANKING: Mejor a Peor Resistencia a la Inflación")
print("(Menor % de aumento = Mejor resistencia)")
print("-" * 100)

for idx, row in brands_df.iterrows():
    emoji = "🏆" if idx == brands_df.index[0] else "🥈" if idx == brands_df.index[1] else "🥉" if idx == brands_df.index[2] else "  "
    print(f"{emoji} {row['brand']:20s} | 2016: ${row['price_2016']:7.2f} → 2025: ${row['price_2025']:7.2f} | "
          f"Cambio: {row['percent_change']:+7.1f}% | Δ${row['absolute_change']:+6.2f}")

print("\n" + "="*100)
print("ANÁLISIS ADICIONAL")
print("="*100)

# Calculate average inflation across all brands
avg_inflation = brands_df['percent_change'].mean()
print(f"\n📈 Inflación promedio en arroz (todas las marcas): {avg_inflation:.1f}%")

# Identify best and worst
best_brand = brands_df.iloc[0]
worst_brand = brands_df.iloc[-1]

print(f"\n🏆 Mejor resistencia: {best_brand['brand']}")
print(f"   • Precio 2016: ${best_brand['price_2016']:.2f}")
print(f"   • Precio 2025: ${best_brand['price_2025']:.2f}")
print(f"   • Aumento: {best_brand['percent_change']:.1f}% ({best_brand['absolute_change']:.2f})")
print(f"   • Diferencia vs promedio: {best_brand['percent_change'] - avg_inflation:.1f} puntos porcentuales")

print(f"\n⚠️  Peor resistencia: {worst_brand['brand']}")
print(f"   • Precio 2016: ${worst_brand['price_2016']:.2f}")
print(f"   • Precio 2025: ${worst_brand['price_2025']:.2f}")
print(f"   • Aumento: {worst_brand['percent_change']:.1f}% ({worst_brand['absolute_change']:.2f})")
print(f"   • Diferencia vs promedio: {worst_brand['percent_change'] - avg_inflation:.1f} puntos porcentuales")

# Group by inflation ranges
print(f"\n📊 Distribución por rangos de inflación:")
low_inflation = brands_df[brands_df['percent_change'] < 50]
medium_inflation = brands_df[(brands_df['percent_change'] >= 50) & (brands_df['percent_change'] < 70)]
high_inflation = brands_df[brands_df['percent_change'] >= 70]

print(f"   • Inflación baja (<50%):   {len(low_inflation)} marcas")
print(f"   • Inflación media (50-70%): {len(medium_inflation)} marcas")
print(f"   • Inflación alta (>70%):   {len(high_inflation)} marcas")

if len(low_inflation) > 0:
    print(f"\n   Marcas con inflación baja (<50%):")
    for _, row in low_inflation.iterrows():
        print(f"   • {row['brand']:20s}: {row['percent_change']:6.1f}%")

print("\n" + "="*100)
