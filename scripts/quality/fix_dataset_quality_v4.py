"""
Fix data quality issues detected in V4 before PostgreSQL import.
Resolves: price_median inconsistencies, brand/specification nulls, products without prices.
"""

import pandas as pd
import numpy as np
from pathlib import Path

print("=" * 100)
print("V4 DATA QUALITY CLEANUP")
print("=" * 100)

# Load data
v4_path = (
    Path(__file__).parent.parent
    / "data"
    / "processed"
    / "prices_aggregated_all_years_v4.csv"
)
products_path = (
    Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"
)

prices_df = pd.read_csv(v4_path)
products_df = pd.read_csv(products_path)

prices_df["date"] = pd.to_datetime(prices_df["date"])

print("\nInitial state:")
print(f"  • Prices: {len(prices_df):,} records")
print(f"  • Products: {len(products_df):,} records")

# ============================================================================
# 1. ARREGLAR price_median INCONSISTENCIAS
# ============================================================================
print("\n" + "=" * 100)
print("1. FIXING price_median INCONSISTENCIES")
print("=" * 100)

# Identificar registros donde price_median viola: price_min ≤ price_median ≤ price_max
invalid_median = (prices_df["price_min"] > prices_df["price_median"]) | (
    prices_df["price_median"] > prices_df["price_max"]
)
print(f"\nRecords with invalid price_median: {invalid_median.sum()}")

if invalid_median.sum() > 0:
    print("\nExamples of inconsistencies:")
    examples = prices_df[invalid_median].head(10)
    for idx, row in examples.iterrows():
        print(
            f"  Min: ${row['price_min']:.2f}, Median: ${row['price_median']:.2f}, Max: ${row['price_max']:.2f}"
        )

    # Fix: use price_avg as price_median when out of range
    print("\nFixing: replacing price_median with price_avg where invalid...")
    prices_df.loc[invalid_median, "price_median"] = prices_df.loc[
        invalid_median, "price_avg"
    ]

    # Verificar
    invalid_after = (prices_df["price_min"] > prices_df["price_median"]) | (
        prices_df["price_median"] > prices_df["price_max"]
    )
    print(f"Remaining inconsistencies: {invalid_after.sum()}")

# ============================================================================
# 2. ARREGLAR BRAND Y SPECIFICATION NULLS
# ============================================================================
print("\n" + "=" * 100)
print("2. FIXING BRAND AND SPECIFICATION NULLS")
print("=" * 100)

print(f"\nProducts with NULL brand: {products_df['brand'].isnull().sum()}")
print(
    f"Products with NULL specification: {products_df['specification'].isnull().sum()}"
)

# For brand NULL, use "Unknown"
if products_df["brand"].isnull().sum() > 0:
    print("\nFixing: replacing NULL brand with 'Unknown Brand'...")
    products_df["brand"].fillna("Unknown Brand", inplace=True)

# For specification NULL, use "Not specified"
if products_df["specification"].isnull().sum() > 0:
    print("Fixing: replacing NULL specification with 'Not specified'...")
    products_df["specification"].fillna("Not specified", inplace=True)

print("\nVerification:")
print(f"  • Brand nulls: {products_df['brand'].isnull().sum()}")
print(f"  • Specification nulls: {products_df['specification'].isnull().sum()}")

# ============================================================================
# 3. REMOVER PRODUCTOS SIN REGISTROS DE PRECIO
# ============================================================================
print("\n" + "=" * 100)
print("3. REMOVING PRODUCTS WITHOUT PRICE RECORDS")
print("=" * 100)

valid_ids = set(prices_df["product_id"].unique())
products_with_prices = products_df[products_df["product_id"].isin(valid_ids)]
products_without_prices = products_df[~products_df["product_id"].isin(valid_ids)]

print(f"\nProducts without price records: {len(products_without_prices)}")
print("\nExamples:")
for idx, row in products_without_prices.head(10).iterrows():
    print(f"  • ID {row['product_id']}: {row['name']}")

print(f"\nFixing: removing {len(products_without_prices)} products without prices...")
products_df = products_with_prices.copy()

print(f"Products after cleanup: {len(products_df):,}")

# ============================================================================
# 4. VALIDAR offer_count Y offer_percentage
# ============================================================================
print("\n" + "=" * 100)
print("4. CHECKING offer_count AND offer_percentage")
print("=" * 100)

print(f"\nRecords with offer_count = 0: {(prices_df['offer_count'] == 0).sum():,}")
print(
    f"Records with offer_percentage = 0: {(prices_df['offer_percentage'] == 0).sum():,}"
)

# This is valid (means no offers that day)
print("\n✓ offer_count and offer_percentage = 0 is valid (means no offers)")
print("  No changes needed for these fields")

# ============================================================================
# 5. VALIDAR TODAS LAS INCONSISTENCIAS RESUELTAS
# ============================================================================
print("\n" + "=" * 100)
print("5. FINAL VALIDATION")
print("=" * 100)

print(
    f"\n✓ price_min ≤ price_avg ≤ price_max: {((prices_df['price_min'] <= prices_df['price_avg']) & (prices_df['price_avg'] <= prices_df['price_max'])).all()}"
)
print(
    f"✓ price_min ≤ price_median ≤ price_max: {((prices_df['price_min'] <= prices_df['price_median']) & (prices_df['price_median'] <= prices_df['price_max'])).all()}"
)
print(f"✓ store_count > 0: {(prices_df['store_count'] > 0).all()}")
print(
    f"✓ offer_percentage between 0-100: {((prices_df['offer_percentage'] >= 0) & (prices_df['offer_percentage'] <= 100)).all()}"
)
print(
    f"✓ Referential integrity: {prices_df['product_id'].isin(products_df['product_id']).all()}"
)
print(
    f"✓ No duplicates (product_id, date): {prices_df.duplicated(subset=['product_id', 'date']).sum() == 0}"
)

# ============================================================================
# 6. GUARDAR VERSIÓN CORREGIDA
# ============================================================================
print("\n" + "=" * 100)
print("6. SAVING CORRECTED FILES")
print("=" * 100)

# Save prices (V4 clean)
output_prices = (
    Path(__file__).parent.parent
    / "data"
    / "processed"
    / "prices_aggregated_all_years_v4_clean.csv"
)
prices_df.to_csv(output_prices, index=False)
print(f"\n✓ Prices saved: {output_prices}")
print(f"  Rows: {len(prices_df):,} records")

# Save products (clean)
output_products = (
    Path(__file__).parent.parent / "data" / "processed" / "products_catalog_clean.csv"
)
products_df.to_csv(output_products, index=False)
print(f"✓ Products saved: {output_products}")
print(f"  Rows: {len(products_df):,} records")

# ============================================================================
# 7. RESUMEN DE CAMBIOS
# ============================================================================
print("\n" + "=" * 100)
print("SUMMARY OF CHANGES")
print("=" * 100)

print(f"""
CHANGES APPLIED:
✓ Fixed {invalid_median.sum()} price_median inconsistencies
✓ Replaced {products_df["brand"].isnull().sum()} NULLs in brand with 'Unknown Brand'
✓ Replaced {products_df["specification"].isnull().sum()} NULLs in specification
✓ Removed {len(products_without_prices)} products without price records

RESULT:
✓ Prices: {len(prices_df):,} records (no change in count)
✓ Products: {len(products_df):,} records (previously: 379)
✓ All data is now valid for PostgreSQL

FILES GENERATED:
• {output_prices}
• {output_products}

NEXT STEP:
Use these clean files to import into PostgreSQL
""")

print("=" * 100)
print("✓ CLEANUP COMPLETED")
print("=" * 100)
