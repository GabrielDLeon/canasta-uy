"""
Análisis de estacionalidad en precios de arroz.
Identifica el mes más barato y más caro para comprar arroz a lo largo del año.
"""

import pandas as pd
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
rice_prices['month_name'] = rice_prices['date'].dt.strftime('%B')
rice_prices['year'] = rice_prices['date'].dt.year

print("="*100)
print("ANÁLISIS DE ESTACIONALIDAD: ARROZ BLANCO")
print("Análisis de precios por mes (agregando todos los años 2016-2025)")
print("="*100)

# Analyze by month across all years
monthly_stats = rice_prices.groupby('month').agg({
    'price_avg': ['mean', 'median', 'std', 'min', 'max', 'count'],
    'price_min': 'mean',
    'price_max': 'mean'
}).round(2)

# Flatten column names
monthly_stats.columns = ['price_avg_mean', 'price_avg_median', 'price_avg_std',
                         'price_avg_min', 'price_avg_max', 'records',
                         'price_min_mean', 'price_max_mean']

month_names = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
               'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

monthly_stats['month_name'] = month_names
monthly_stats = monthly_stats[['month_name', 'price_avg_mean', 'price_avg_median', 'price_avg_std',
                                'price_min_mean', 'price_max_mean', 'records']]

print("\n📊 ESTADÍSTICAS POR MES")
print("-" * 100)
print(f"{'Mes':<15} {'Promedio':>12} {'Mediana':>12} {'Desv. Est':>12} {'Min':>10} {'Max':>10} {'Registros':>10}")
print("-" * 100)

for idx, row in monthly_stats.iterrows():
    print(f"{row['month_name']:<15} ${row['price_avg_mean']:>10.2f} ${row['price_avg_median']:>10.2f} "
          f"${row['price_avg_std']:>10.2f} ${row['price_min_mean']:>8.2f} ${row['price_max_mean']:>8.2f} "
          f"{int(row['records']):>10,}")

# Find cheapest and most expensive months
cheapest_month_idx = monthly_stats['price_avg_mean'].idxmin()
most_expensive_month_idx = monthly_stats['price_avg_mean'].idxmax()

cheapest_month = monthly_stats.loc[cheapest_month_idx]
most_expensive_month = monthly_stats.loc[most_expensive_month_idx]

print("\n" + "="*100)
print("HALLAZGOS CLAVE")
print("="*100)

print(f"\n💰 MES MÁS BARATO: {cheapest_month['month_name']}")
print(f"   • Precio promedio: ${cheapest_month['price_avg_mean']:.2f}")
print(f"   • Precio mediano: ${cheapest_month['price_avg_median']:.2f}")
print(f"   • Rango de precios: ${cheapest_month['price_min_mean']:.2f} - ${cheapest_month['price_max_mean']:.2f}")

print(f"\n🔴 MES MÁS CARO: {most_expensive_month['month_name']}")
print(f"   • Precio promedio: ${most_expensive_month['price_avg_mean']:.2f}")
print(f"   • Precio mediano: ${most_expensive_month['price_avg_median']:.2f}")
print(f"   • Rango de precios: ${most_expensive_month['price_min_mean']:.2f} - ${most_expensive_month['price_max_mean']:.2f}")

# Calculate savings
savings = most_expensive_month['price_avg_mean'] - cheapest_month['price_avg_mean']
savings_pct = (savings / most_expensive_month['price_avg_mean']) * 100

print(f"\n📉 AHORRO POTENCIAL")
print(f"   • Diferencia: ${savings:.2f}")
print(f"   • Porcentaje de ahorro: {savings_pct:.1f}%")

# Analyze by season
print("\n" + "="*100)
print("ANÁLISIS POR TRIMESTRE")
print("="*100)

rice_prices['quarter'] = rice_prices['date'].dt.quarter
quarterly = rice_prices.groupby('quarter')['price_avg'].agg(['mean', 'median', 'std']).round(2)
quarter_names = {1: 'Q1 (Ene-Mar)', 2: 'Q2 (Abr-Jun)', 3: 'Q3 (Jul-Sep)', 4: 'Q4 (Oct-Dic)'}

print(f"\n{'Trimestre':<15} {'Promedio':>12} {'Mediana':>12} {'Desv. Est':>12}")
print("-" * 50)
for q in [1, 2, 3, 4]:
    row = quarterly.loc[q]
    print(f"{quarter_names[q]:<15} ${row['mean']:>10.2f} ${row['median']:>10.2f} ${row['std']:>10.2f}")

# Find best and worst quarters
best_quarter = quarterly['mean'].idxmin()
worst_quarter = quarterly['mean'].idxmax()

quarter_savings = quarterly.loc[worst_quarter, 'mean'] - quarterly.loc[best_quarter, 'mean']

print(f"\n🏆 Trimestre más barato: {quarter_names[best_quarter]} (${quarterly.loc[best_quarter, 'mean']:.2f})")
print(f"⚠️  Trimestre más caro: {quarter_names[worst_quarter]} (${quarterly.loc[worst_quarter, 'mean']:.2f})")
print(f"💰 Ahorro potencial: ${quarter_savings:.2f}")

# Year-over-year seasonality
print("\n" + "="*100)
print("CONSISTENCIA DE PATRÓN ESTACIONAL")
print("Variabilidad del precio promedio mensual a lo largo de los años")
print("="*100)

# For each month, show price range across years
print(f"\n{'Mes':<15} {'Mín (año)':>20} {'Máx (año)':>20} {'Variación':>12}")
print("-" * 70)

for month in range(1, 13):
    month_data = rice_prices[rice_prices['month'] == month].groupby('year')['price_avg'].mean()
    if len(month_data) > 0:
        min_price = month_data.min()
        max_price = month_data.max()
        min_year = month_data.idxmin()
        max_year = month_data.idxmax()
        variation = max_price - min_price

        print(f"{month_names[month-1]:<15} ${min_price:>7.2f} ({min_year}) → ${max_price:>7.2f} ({max_year}) │ ${variation:>10.2f}")

print("\n" + "="*100)
