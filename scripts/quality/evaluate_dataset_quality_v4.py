"""
Full quality evaluation of the V4 dataset before PostgreSQL migration.
Checks: nulls, data types, ranges, duplicates, referential integrity, etc.
"""

import pandas as pd
import numpy as np
from pathlib import Path
from datetime import datetime

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

print("=" * 100)
print("V4 DATASET QUALITY EVALUATION FOR POSTGRESQL MIGRATION")
print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("=" * 100)

# ============================================================================
# 1. ESTRUCTURA Y TIPOS DE DATOS
# ============================================================================
print("\n1️⃣  STRUCTURE AND DATA TYPES")
print("-" * 100)

print("\nPRICES TABLE (V4):")
print(f"   Rows: {len(prices_df):,}")
print(f"   Columns: {len(prices_df.columns)}")
print("\n   Structure:")
for col in prices_df.columns:
    print(
        f"   • {col:<20} {str(prices_df[col].dtype):<15} (Nulls: {prices_df[col].isnull().sum():,})"
    )

print("\nPRODUCTS TABLE:")
print(f"   Rows: {len(products_df):,}")
print(f"   Columns: {len(products_df.columns)}")
print("\n   Structure:")
for col in products_df.columns:
    print(
        f"   • {col:<20} {str(products_df[col].dtype):<15} (Nulls: {products_df[col].isnull().sum():,})"
    )

# ============================================================================
# 2. VERIFICACIÓN DE NULLS Y VALORES FALTANTES
# ============================================================================
print("\n" + "=" * 100)
print("2️⃣  NULLS AND MISSING VALUES")
print("-" * 100)

print("\nPRICES TABLE:")
null_summary = prices_df.isnull().sum()
if null_summary.sum() == 0:
    print("   ✓ No NULL values detected")
else:
    print("   ⚠️  NULL values found:")
    for col, count in null_summary[null_summary > 0].items():
        print(f"   • {col}: {count:,} ({count / len(prices_df) * 100:.2f}%)")

print("\nPRODUCTS TABLE:")
null_summary_prod = products_df.isnull().sum()
if null_summary_prod.sum() == 0:
    print("   ✓ No NULL values detected")
else:
    print("   ⚠️  NULL values found:")
    for col, count in null_summary_prod[null_summary_prod > 0].items():
        print(f"   • {col}: {count:,} ({count / len(products_df) * 100:.2f}%)")

# ============================================================================
# 3. ANÁLISIS DE VALORES NUMÉRICOS
# ============================================================================
print("\n" + "=" * 100)
print("3️⃣  NUMERIC VALUES ANALYSIS")
print("-" * 100)

numeric_cols = [
    "price_min",
    "price_max",
    "price_avg",
    "price_median",
    "price_std",
    "store_count",
    "offer_count",
    "offer_percentage",
]

print("\nValue ranges (PRICES):")
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
            print(f"   ⚠️  WARNING: {(prices_df[col] < 0).sum()} negative values")

        # Check for zeros (might be problematic for some fields)
        if (prices_df[col] == 0).any():
            zero_count = (prices_df[col] == 0).sum()
            print(f"   ⚠️  {zero_count:,} zero values")

# ============================================================================
# 4. INTEGRIDAD REFERENCIAL
# ============================================================================
print("\n" + "=" * 100)
print("4️⃣  REFERENTIAL INTEGRITY")
print("-" * 100)

print("\nProduct_id validation:")
valid_ids = set(products_df["product_id"].unique())
used_ids = set(prices_df["product_id"].unique())

print(f"   • Product IDs available: {len(valid_ids)}")
print(f"   • Product IDs used in prices: {len(used_ids)}")
print(f"   • Valid IDs used: {len(used_ids & valid_ids)}")

orphan_ids = used_ids - valid_ids
if len(orphan_ids) > 0:
    print(
        f"   ⚠️  WARNING: {len(orphan_ids)} orphan product_ids (no product): {orphan_ids}"
    )
else:
    print("   ✓ All product_id references are valid")

missing_ids = valid_ids - used_ids
if len(missing_ids) > 0:
    print(f"   ℹ️  {len(missing_ids)} products have no price records: {missing_ids}")

# ============================================================================
# 5. DUPLICADOS
# ============================================================================
print("\n" + "=" * 100)
print("5️⃣  DUPLICATE DETECTION")
print("-" * 100)

print("\nPRICES TABLE:")
# Check for exact duplicates
exact_dupes = prices_df.duplicated().sum()
print(f"   • Exact duplicate rows: {exact_dupes}")

# Check for logical duplicates (same product, date, but different metrics)
if "date" in prices_df.columns and "product_id" in prices_df.columns:
    logical_dupes = prices_df.groupby(["product_id", "date"]).size()
    multi_records = (logical_dupes > 1).sum()
    if multi_records > 0:
        print(
            f"   ⚠️  {multi_records} (product_id, date) combinations with multiple records"
        )
        print("      Example:")
        dupe_example = prices_df[
            prices_df.duplicated(subset=["product_id", "date"], keep=False)
        ].head()
        for idx, row in dupe_example.iterrows():
            print(f"      Product {row['product_id']}, Date {row['date']}")
    else:
        print("   ✓ Each (product_id, date) combination is unique")

print("\nPRODUCTS TABLE:")
prod_dupes = products_df.duplicated(subset=["product_id"]).sum()
if prod_dupes > 0:
    print(f"   ⚠️  {prod_dupes} duplicate product_ids")
else:
    print("   ✓ All product_ids are unique")

# ============================================================================
# 6. RANGO DE FECHAS
# ============================================================================
print("\n" + "=" * 100)
print("6️⃣  TIME COVERAGE")
print("-" * 100)

prices_df["date"] = pd.to_datetime(prices_df["date"])
print("\nDate range:")
print(f"   • First date: {prices_df['date'].min().date()}")
print(f"   • Last date: {prices_df['date'].max().date()}")
print(f"   • Total days: {(prices_df['date'].max() - prices_df['date'].min()).days}")
print(f"   • Years covered: {sorted(prices_df['date'].dt.year.unique())}")

# Check for gaps in data
print("\n   Coverage by year:")
for year in sorted(prices_df["date"].dt.year.unique()):
    year_data = prices_df[prices_df["date"].dt.year == year]
    months = year_data["date"].dt.month.nunique()
    records = len(year_data)
    print(f"   • {year}: {records:,} records ({months} unique months)")

# ============================================================================
# 7. CONSISTENCIA DE DATOS
# ============================================================================
print("\n" + "=" * 100)
print("7️⃣  DATA CONSISTENCY")
print("-" * 100)

print("\nLogical validations:")

# price_min <= price_avg <= price_max
valid_prices = (prices_df["price_min"] <= prices_df["price_avg"]) & (
    prices_df["price_avg"] <= prices_df["price_max"]
)
if valid_prices.all():
    print("   ✓ price_min ≤ price_avg ≤ price_max (OK for all records)")
else:
    print(
        f"   ⚠️  {(~valid_prices).sum()} records violate price_min ≤ price_avg ≤ price_max"
    )

# price_median should be between min and max
valid_median = (prices_df["price_min"] <= prices_df["price_median"]) & (
    prices_df["price_median"] <= prices_df["price_max"]
)
if valid_median.all():
    print("   ✓ price_min ≤ price_median ≤ price_max (OK for all records)")
else:
    print(
        f"   ⚠️  {(~valid_median).sum()} records violate price_min ≤ price_median ≤ price_max"
    )

# store_count and offer_count should be positive
if (prices_df["store_count"] > 0).all():
    print("   ✓ store_count is always positive")
else:
    print(f"   ⚠️  {(prices_df['store_count'] <= 0).sum()} records with store_count ≤ 0")

if (prices_df["offer_count"] > 0).all():
    print("   ✓ offer_count is always positive")
else:
    print(f"   ⚠️  {(prices_df['offer_count'] <= 0).sum()} records with offer_count ≤ 0")

# offer_percentage between 0 and 100
if (
    (prices_df["offer_percentage"] >= 0) & (prices_df["offer_percentage"] <= 100)
).all():
    print("   ✓ offer_percentage is between 0 and 100")
else:
    invalid_pct = (
        (prices_df["offer_percentage"] < 0) | (prices_df["offer_percentage"] > 100)
    ).sum()
    print(f"   ⚠️  {invalid_pct} records with offer_percentage outside [0, 100]")

# ============================================================================
# 8. OUTLIERS REMOVIDOS
# ============================================================================
print("\n" + "=" * 100)
print("8. CLEANING QUALITY (OUTLIERS REMOVED)")
print("-" * 100)

# Load V1 to compare
v1_path = (
    Path(__file__).parent.parent
    / "data"
    / "processed"
    / "prices_aggregated_all_years.csv"
)
if v1_path.exists():
    v1_df = pd.read_csv(v1_path)
    removed = len(v1_df) - len(prices_df)
    removal_pct = (removed / len(v1_df)) * 100

    print("\nV1 to V4 comparison:")
    print(f"   • V1 records (raw): {len(v1_df):,}")
    print(f"   • V4 records (clean): {len(prices_df):,}")
    print(f"   • Records removed: {removed:,} ({removal_pct:.2f}%)")
    print(f"   • Records retained: {len(prices_df):,} ({100 - removal_pct:.2f}%)")

# ============================================================================
# 9. ESTADÍSTICAS FINALES
# ============================================================================
print("\n" + "=" * 100)
print("9. FINAL STATISTICS")
print("-" * 100)

print("\nData summary:")
print(f"   • Total records: {len(prices_df):,}")
print(f"   • Total unique products: {prices_df['product_id'].nunique()}")
print(f"   • Total unique dates: {prices_df['date'].nunique()}")
print(
    f"   • Price range (price_avg): ${prices_df['price_avg'].min():.2f} - ${prices_df['price_avg'].max():.2f}"
)
print(f"   • Average stores per record: {prices_df['store_count'].mean():.1f}")
print(f"   • Average offers per record: {prices_df['offer_count'].mean():.1f}")

# ============================================================================
# 10. RECOMENDACIONES
# ============================================================================
print("\n" + "=" * 100)
print("10. RECOMMENDATIONS FOR POSTGRESQL")
print("=" * 100)

issues = []

# Check all the issues we found
if not valid_prices.all():
    issues.append("• Inconsistencies in price_min ≤ price_avg ≤ price_max")

if not valid_median.all():
    issues.append("• Inconsistencies in price_median")

if (prices_df["store_count"] <= 0).any():
    issues.append("• Invalid values in store_count")

if (prices_df["offer_count"] <= 0).any():
    issues.append("• Invalid values in offer_count")

if not (
    (
        (prices_df["offer_percentage"] >= 0) & (prices_df["offer_percentage"] <= 100)
    ).all()
):
    issues.append("• Invalid values in offer_percentage")

if len(orphan_ids) > 0:
    issues.append("• Orphan product_ids (missing from products.csv)")

if issues:
    print("\nISSUES FOUND:")
    for issue in issues:
        print(issue)
    print("\nRECOMMENDATION: Resolve these issues before migrating to PostgreSQL")
else:
    print("\nNo critical issues detected")

# Schema recommendation
print("\nRECOMMENDED POSTGRESQL SCHEMA:")
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

print("\n" + "=" * 100)
print("EVALUATION COMPLETED")
print("=" * 100)
