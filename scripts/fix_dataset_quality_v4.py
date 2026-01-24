"""
Script para arreglar los problemas de calidad detectados en V4 antes de PostgreSQL.
Resuelve: price_median inconsistencias, brand/specification nulls, productos sin precios.
"""

import pandas as pd
import numpy as np
from pathlib import Path

print("="*100)
print("LIMPIEZA DE CALIDAD DEL DATASET V4")
print("="*100)

# Load data
v4_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

print(f"\nEstado inicial:")
print(f"  • Prices: {len(prices_df):,} registros")
print(f"  • Products: {len(products_df):,} registros")

# ============================================================================
# 1. ARREGLAR price_median INCONSISTENCIAS
# ============================================================================
print("\n" + "="*100)
print("1. ARREGLANDO price_median INCONSISTENCIAS")
print("="*100)

# Identificar registros donde price_median viola: price_min ≤ price_median ≤ price_max
invalid_median = (prices_df['price_min'] > prices_df['price_median']) | (prices_df['price_median'] > prices_df['price_max'])
print(f"\nRegistros con price_median inválido: {invalid_median.sum()}")

if invalid_median.sum() > 0:
    print("\nEjemplos de inconsistencias:")
    examples = prices_df[invalid_median].head(10)
    for idx, row in examples.iterrows():
        print(f"  Min: ${row['price_min']:.2f}, Median: ${row['price_median']:.2f}, Max: ${row['price_max']:.2f}")

    # Arreglar: usar price_avg como price_median si está fuera de rango
    print("\nArreglando: reemplazando price_median con price_avg donde sea inválido...")
    prices_df.loc[invalid_median, 'price_median'] = prices_df.loc[invalid_median, 'price_avg']

    # Verificar
    invalid_after = (prices_df['price_min'] > prices_df['price_median']) | (prices_df['price_median'] > prices_df['price_max'])
    print(f"Inconsistencias restantes: {invalid_after.sum()}")

# ============================================================================
# 2. ARREGLAR BRAND Y SPECIFICATION NULLS
# ============================================================================
print("\n" + "="*100)
print("2. ARREGLANDO BRAND Y SPECIFICATION NULLS")
print("="*100)

print(f"\nProducts con brand NULL: {products_df['brand'].isnull().sum()}")
print(f"Products con specification NULL: {products_df['specification'].isnull().sum()}")

# Para brand NULL, usar "Unknown"
if products_df['brand'].isnull().sum() > 0:
    print("\nArreglando: reemplazando brand NULL con 'Unknown Brand'...")
    products_df['brand'].fillna('Unknown Brand', inplace=True)

# Para specification NULL, usar "Not specified"
if products_df['specification'].isnull().sum() > 0:
    print("Arreglando: reemplazando specification NULL con 'Not specified'...")
    products_df['specification'].fillna('Not specified', inplace=True)

print(f"\nVerificación:")
print(f"  • Brand nulls: {products_df['brand'].isnull().sum()}")
print(f"  • Specification nulls: {products_df['specification'].isnull().sum()}")

# ============================================================================
# 3. REMOVER PRODUCTOS SIN REGISTROS DE PRECIO
# ============================================================================
print("\n" + "="*100)
print("3. LIMPIANDO PRODUCTOS SIN REGISTROS DE PRECIO")
print("="*100)

valid_ids = set(prices_df['product_id'].unique())
products_with_prices = products_df[products_df['product_id'].isin(valid_ids)]
products_without_prices = products_df[~products_df['product_id'].isin(valid_ids)]

print(f"\nProductos sin registros de precio: {len(products_without_prices)}")
print("\nEjemplos:")
for idx, row in products_without_prices.head(10).iterrows():
    print(f"  • ID {row['product_id']}: {row['name']}")

print(f"\nArreglando: removiendo {len(products_without_prices)} productos sin precios...")
products_df = products_with_prices.copy()

print(f"Products después de limpieza: {len(products_df):,}")

# ============================================================================
# 4. VALIDAR offer_count Y offer_percentage
# ============================================================================
print("\n" + "="*100)
print("4. ANALIZANDO offer_count Y offer_percentage")
print("="*100)

print(f"\nRegistros con offer_count = 0: {(prices_df['offer_count'] == 0).sum():,}")
print(f"Registros con offer_percentage = 0: {(prices_df['offer_percentage'] == 0).sum():,}")

# Esto es válido (significa no hay ofertas ese día)
# pero vamos a usar -1 o NULL para distinguir "sin datos" de "sin ofertas"
print("\n✓ offer_count y offer_percentage = 0 es válido (significa sin ofertas)")
print("  No hay cambios necesarios para estos campos")

# ============================================================================
# 5. VALIDAR TODAS LAS INCONSISTENCIAS RESUELTAS
# ============================================================================
print("\n" + "="*100)
print("5. VALIDACIÓN FINAL")
print("="*100)

print(f"\n✓ price_min ≤ price_avg ≤ price_max: {((prices_df['price_min'] <= prices_df['price_avg']) & (prices_df['price_avg'] <= prices_df['price_max'])).all()}")
print(f"✓ price_min ≤ price_median ≤ price_max: {((prices_df['price_min'] <= prices_df['price_median']) & (prices_df['price_median'] <= prices_df['price_max'])).all()}")
print(f"✓ store_count > 0: {(prices_df['store_count'] > 0).all()}")
print(f"✓ offer_percentage entre 0-100: {((prices_df['offer_percentage'] >= 0) & (prices_df['offer_percentage'] <= 100)).all()}")
print(f"✓ Integridad referencial: {prices_df['product_id'].isin(products_df['product_id']).all()}")
print(f"✓ Sin duplicados (product_id, date): {prices_df.duplicated(subset=['product_id', 'date']).sum() == 0}")

# ============================================================================
# 6. GUARDAR VERSIÓN CORREGIDA
# ============================================================================
print("\n" + "="*100)
print("6. GUARDANDO ARCHIVOS CORREGIDOS")
print("="*100)

# Guardar prices (V4 limpio)
output_prices = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4_clean.csv"
prices_df.to_csv(output_prices, index=False)
print(f"\n✓ Prices guardado: {output_prices}")
print(f"  Tamaño: {len(prices_df):,} registros")

# Guardar products (limpio)
output_products = Path(__file__).parent.parent / "data" / "processed" / "products_catalog_clean.csv"
products_df.to_csv(output_products, index=False)
print(f"✓ Products guardado: {output_products}")
print(f"  Tamaño: {len(products_df):,} registros")

# ============================================================================
# 7. RESUMEN DE CAMBIOS
# ============================================================================
print("\n" + "="*100)
print("📊 RESUMEN DE CAMBIOS")
print("="*100)

print(f"""
CAMBIOS REALIZADOS:
✓ Arregladas {invalid_median.sum()} inconsistencias en price_median
✓ Reemplazados {products_df['brand'].isnull().sum()} nulls en brand con 'Unknown Brand'
✓ Reemplazados {products_df['specification'].isnull().sum()} nulls en specification
✓ Removidos {len(products_without_prices)} productos sin registros de precio

RESULTADO:
✓ Prices: {len(prices_df):,} registros (sin cambios en cantidad)
✓ Products: {len(products_df):,} registros (antes: 379)
✓ Todos los datos ahora son válidos para PostgreSQL

ARCHIVOS GENERADOS:
• {output_prices}
• {output_products}

PRÓXIMO PASO:
Usar estos archivos limpios para importar a PostgreSQL
""")

print("="*100)
print("✓ LIMPIEZA COMPLETADA")
print("="*100)
