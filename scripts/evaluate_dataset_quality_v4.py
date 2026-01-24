"""
Evaluación completa de calidad del dataset V4 antes de migrarlo a PostgreSQL.
Verifica: nulls, tipos de datos, rangos, duplicados, integridad referencial, etc.
"""

import pandas as pd
import numpy as np
from pathlib import Path
from datetime import datetime

# Load data
v4_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v4.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

print("="*100)
print("EVALUACIÓN DE CALIDAD DEL DATASET V4 PARA MIGRACIÓN A POSTGRESQL")
print(f"Fecha: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("="*100)

# ============================================================================
# 1. ESTRUCTURA Y TIPOS DE DATOS
# ============================================================================
print("\n1️⃣  ESTRUCTURA Y TIPOS DE DATOS")
print("-" * 100)

print("\n📋 TABLA PRICES (V4):")
print(f"   Filas: {len(prices_df):,}")
print(f"   Columnas: {len(prices_df.columns)}")
print(f"\n   Estructura:")
for col in prices_df.columns:
    print(f"   • {col:<20} {str(prices_df[col].dtype):<15} (Nulls: {prices_df[col].isnull().sum():,})")

print("\n📋 TABLA PRODUCTS:")
print(f"   Filas: {len(products_df):,}")
print(f"   Columnas: {len(products_df.columns)}")
print(f"\n   Estructura:")
for col in products_df.columns:
    print(f"   • {col:<20} {str(products_df[col].dtype):<15} (Nulls: {products_df[col].isnull().sum():,})")

# ============================================================================
# 2. VERIFICACIÓN DE NULLS Y VALORES FALTANTES
# ============================================================================
print("\n" + "="*100)
print("2️⃣  VERIFICACIÓN DE NULLS Y VALORES FALTANTES")
print("-" * 100)

print("\n📊 TABLA PRICES:")
null_summary = prices_df.isnull().sum()
if null_summary.sum() == 0:
    print("   ✓ Sin valores NULL detectados")
else:
    print("   ⚠️  Valores NULL encontrados:")
    for col, count in null_summary[null_summary > 0].items():
        print(f"   • {col}: {count:,} ({count/len(prices_df)*100:.2f}%)")

print("\n📊 TABLA PRODUCTS:")
null_summary_prod = products_df.isnull().sum()
if null_summary_prod.sum() == 0:
    print("   ✓ Sin valores NULL detectados")
else:
    print("   ⚠️  Valores NULL encontrados:")
    for col, count in null_summary_prod[null_summary_prod > 0].items():
        print(f"   • {col}: {count:,} ({count/len(products_df)*100:.2f}%)")

# ============================================================================
# 3. ANÁLISIS DE VALORES NUMÉRICOS
# ============================================================================
print("\n" + "="*100)
print("3️⃣  ANÁLISIS DE VALORES NUMÉRICOS")
print("-" * 100)

numeric_cols = ['price_min', 'price_max', 'price_avg', 'price_median', 'price_std', 'store_count', 'offer_count', 'offer_percentage']

print("\n📊 Rango de valores (PRICES):")
for col in numeric_cols:
    if col in prices_df.columns:
        print(f"\n   {col}:")
        print(f"   • Min: {prices_df[col].min():.2f}")
        print(f"   • Max: {prices_df[col].max():.2f}")
        print(f"   • Mean: {prices_df[col].mean():.2f}")
        print(f"   • Median: {prices_df[col].median():.2f}")
        print(f"   • Std: {prices_df[col].std():.2f}")

        # Check for negative values
        if (prices_df[col] < 0).any():
            print(f"   ⚠️  ADVERTENCIA: {(prices_df[col] < 0).sum()} valores negativos")

        # Check for zeros (might be problematic for some fields)
        if (prices_df[col] == 0).any():
            zero_count = (prices_df[col] == 0).sum()
            print(f"   ⚠️  {zero_count:,} valores en cero")

# ============================================================================
# 4. INTEGRIDAD REFERENCIAL
# ============================================================================
print("\n" + "="*100)
print("4️⃣  INTEGRIDAD REFERENCIAL")
print("-" * 100)

print("\n🔗 Validación de product_id:")
valid_ids = set(products_df['product_id'].unique())
used_ids = set(prices_df['product_id'].unique())

print(f"   • Product IDs disponibles: {len(valid_ids)}")
print(f"   • Product IDs usados en prices: {len(used_ids)}")
print(f"   • IDs válidos usados: {len(used_ids & valid_ids)}")

orphan_ids = used_ids - valid_ids
if len(orphan_ids) > 0:
    print(f"   ⚠️  ADVERTENCIA: {len(orphan_ids)} product_ids huérfanos (sin producto): {orphan_ids}")
else:
    print(f"   ✓ Todas las referencias de product_id son válidas")

missing_ids = valid_ids - used_ids
if len(missing_ids) > 0:
    print(f"   ℹ️  {len(missing_ids)} productos no tienen registros de precio: {missing_ids}")

# ============================================================================
# 5. DUPLICADOS
# ============================================================================
print("\n" + "="*100)
print("5️⃣  DETECCIÓN DE DUPLICADOS")
print("-" * 100)

print("\n📋 TABLA PRICES:")
# Check for exact duplicates
exact_dupes = prices_df.duplicated().sum()
print(f"   • Filas exactamente duplicadas: {exact_dupes}")

# Check for logical duplicates (same product, date, but different metrics)
if 'date' in prices_df.columns and 'product_id' in prices_df.columns:
    logical_dupes = prices_df.groupby(['product_id', 'date']).size()
    multi_records = (logical_dupes > 1).sum()
    if multi_records > 0:
        print(f"   ⚠️  {multi_records} combinaciones de (product_id, date) con múltiples registros")
        print(f"      Ejemplo:")
        dupe_example = prices_df[prices_df.duplicated(subset=['product_id', 'date'], keep=False)].head()
        for idx, row in dupe_example.iterrows():
            print(f"      Product {row['product_id']}, Date {row['date']}")
    else:
        print(f"   ✓ Cada combinación (product_id, date) es única")

print("\n📋 TABLA PRODUCTS:")
prod_dupes = products_df.duplicated(subset=['product_id']).sum()
if prod_dupes > 0:
    print(f"   ⚠️  {prod_dupes} product_ids duplicados")
else:
    print(f"   ✓ Todos los product_ids son únicos")

# ============================================================================
# 6. RANGO DE FECHAS
# ============================================================================
print("\n" + "="*100)
print("6️⃣  COBERTURA TEMPORAL")
print("-" * 100)

prices_df['date'] = pd.to_datetime(prices_df['date'])
print(f"\n📅 Rango de fechas:")
print(f"   • Primera fecha: {prices_df['date'].min().date()}")
print(f"   • Última fecha: {prices_df['date'].max().date()}")
print(f"   • Días totales: {(prices_df['date'].max() - prices_df['date'].min()).days}")
print(f"   • Años cubiertos: {sorted(prices_df['date'].dt.year.unique())}")

# Check for gaps in data
print(f"\n   Cobertura por año:")
for year in sorted(prices_df['date'].dt.year.unique()):
    year_data = prices_df[prices_df['date'].dt.year == year]
    months = year_data['date'].dt.month.nunique()
    records = len(year_data)
    print(f"   • {year}: {records:,} registros ({months} meses únicos)")

# ============================================================================
# 7. CONSISTENCIA DE DATOS
# ============================================================================
print("\n" + "="*100)
print("7️⃣  CONSISTENCIA DE DATOS")
print("-" * 100)

print("\n🔍 Validaciones lógicas:")

# price_min <= price_avg <= price_max
valid_prices = (prices_df['price_min'] <= prices_df['price_avg']) & (prices_df['price_avg'] <= prices_df['price_max'])
if valid_prices.all():
    print(f"   ✓ price_min ≤ price_avg ≤ price_max (OK en todos los registros)")
else:
    print(f"   ⚠️  {(~valid_prices).sum()} registros violan price_min ≤ price_avg ≤ price_max")

# price_median should be between min and max
valid_median = (prices_df['price_min'] <= prices_df['price_median']) & (prices_df['price_median'] <= prices_df['price_max'])
if valid_median.all():
    print(f"   ✓ price_min ≤ price_median ≤ price_max (OK en todos los registros)")
else:
    print(f"   ⚠️  {(~valid_median).sum()} registros violan price_min ≤ price_median ≤ price_max")

# store_count and offer_count should be positive
if (prices_df['store_count'] > 0).all():
    print(f"   ✓ store_count es siempre positivo")
else:
    print(f"   ⚠️  {(prices_df['store_count'] <= 0).sum()} registros con store_count ≤ 0")

if (prices_df['offer_count'] > 0).all():
    print(f"   ✓ offer_count es siempre positivo")
else:
    print(f"   ⚠️  {(prices_df['offer_count'] <= 0).sum()} registros con offer_count ≤ 0")

# offer_percentage between 0 and 100
if ((prices_df['offer_percentage'] >= 0) & (prices_df['offer_percentage'] <= 100)).all():
    print(f"   ✓ offer_percentage está entre 0 y 100")
else:
    invalid_pct = ((prices_df['offer_percentage'] < 0) | (prices_df['offer_percentage'] > 100)).sum()
    print(f"   ⚠️  {invalid_pct} registros con offer_percentage fuera de rango [0, 100]")

# ============================================================================
# 8. OUTLIERS REMOVIDOS
# ============================================================================
print("\n" + "="*100)
print("8️⃣  CALIDAD DE LIMPIEZA (OUTLIERS REMOVIDOS)")
print("-" * 100)

# Load V1 to compare
v1_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years.csv"
if v1_path.exists():
    v1_df = pd.read_csv(v1_path)
    removed = len(v1_df) - len(prices_df)
    removal_pct = (removed / len(v1_df)) * 100

    print(f"\n📊 Comparación V1 → V4:")
    print(f"   • Registros V1 (sin limpiar): {len(v1_df):,}")
    print(f"   • Registros V4 (limpio): {len(prices_df):,}")
    print(f"   • Registros removidos: {removed:,} ({removal_pct:.2f}%)")
    print(f"   • Registros retenidos: {len(prices_df):,} ({100-removal_pct:.2f}%)")

# ============================================================================
# 9. ESTADÍSTICAS FINALES
# ============================================================================
print("\n" + "="*100)
print("9️⃣  ESTADÍSTICAS FINALES")
print("-" * 100)

print(f"\n📊 Resumen de datos:")
print(f"   • Total de registros: {len(prices_df):,}")
print(f"   • Total de productos únicos: {prices_df['product_id'].nunique()}")
print(f"   • Total de fechas únicas: {prices_df['date'].nunique()}")
print(f"   • Rango de precios (price_avg): ${prices_df['price_avg'].min():.2f} - ${prices_df['price_avg'].max():.2f}")
print(f"   • Promedio de tiendas por registro: {prices_df['store_count'].mean():.1f}")
print(f"   • Promedio de ofertas por registro: {prices_df['offer_count'].mean():.1f}")

# ============================================================================
# 10. RECOMENDACIONES
# ============================================================================
print("\n" + "="*100)
print("🎯 RECOMENDACIONES PARA POSTGRESQL")
print("="*100)

issues = []

# Check all the issues we found
if not valid_prices.all():
    issues.append("• Hay inconsistencias en relaciones price_min ≤ price_avg ≤ price_max")

if not valid_median.all():
    issues.append("• Hay inconsistencias en price_median")

if (prices_df['store_count'] <= 0).any():
    issues.append("• Hay valores inválidos en store_count")

if (prices_df['offer_count'] <= 0).any():
    issues.append("• Hay valores inválidos en offer_count")

if not (((prices_df['offer_percentage'] >= 0) & (prices_df['offer_percentage'] <= 100)).all()):
    issues.append("• Hay valores inválidos en offer_percentage")

if len(orphan_ids) > 0:
    issues.append("• Hay product_ids huérfanos (sin producto en products.csv)")

if issues:
    print("\n⚠️  PROBLEMAS ENCONTRADOS:")
    for issue in issues:
        print(issue)
    print("\n❌ RECOMENDACIÓN: Resolver estos problemas antes de migrar a PostgreSQL")
else:
    print("\n✅ NO HAY PROBLEMAS CRÍTICOS DETECTADOS")

# Schema recommendation
print("\n📋 ESQUEMA POSTGRESQL RECOMENDADO:")
print("""
CREATE TABLE products (
    product_id INT PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    specification VARCHAR(500),
    name VARCHAR(500) NOT NULL
);

CREATE TABLE prices (
    price_id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(product_id),
    date DATE NOT NULL,
    price_min DECIMAL(10, 2) NOT NULL,
    price_max DECIMAL(10, 2) NOT NULL,
    price_avg DECIMAL(10, 2) NOT NULL,
    price_median DECIMAL(10, 2) NOT NULL,
    price_std DECIMAL(10, 2) NOT NULL,
    store_count INT NOT NULL,
    offer_count INT NOT NULL,
    offer_percentage DECIMAL(5, 2) NOT NULL,
    UNIQUE(product_id, date)
);

CREATE INDEX idx_prices_date ON prices(date);
CREATE INDEX idx_prices_product ON prices(product_id);
CREATE INDEX idx_prices_product_date ON prices(product_id, date);
""")

print("\n" + "="*100)
print("✓ EVALUACIÓN COMPLETADA")
print("="*100)
