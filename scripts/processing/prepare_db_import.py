"""
prepare_db_import.py

Transforma los CSVs V4 clean al formato correcto para importar a PostgreSQL.

Input:
- data/processed/products_catalog_clean.csv
- data/processed/prices_aggregated_all_years_v4_clean.csv

Output:
- data/processed/db_import/categories.csv
- data/processed/db_import/products_db.csv
- data/processed/db_import/prices_db.csv

Uso:
    uv run python scripts/prepare_db_import.py
"""

import pandas as pd
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).parent.parent
PROCESSED_DIR = BASE_DIR / "data" / "processed"
DB_IMPORT_DIR = PROCESSED_DIR / "db_import"

# Input files
PRODUCTS_INPUT = PROCESSED_DIR / "products_catalog_clean.csv"
PRICES_INPUT = PROCESSED_DIR / "prices_aggregated_all_years_v4_clean.csv"

# Output files
CATEGORIES_OUTPUT = DB_IMPORT_DIR / "categories.csv"
PRODUCTS_OUTPUT = DB_IMPORT_DIR / "products_db.csv"
PRICES_OUTPUT = DB_IMPORT_DIR / "prices_db.csv"


def main():
    print("=" * 60)
    print("PREPARE DB IMPORT - Transforming CSVs for PostgreSQL")
    print("=" * 60)

    # Create output directory
    DB_IMPORT_DIR.mkdir(parents=True, exist_ok=True)
    print(f"\n✓ Output directory: {DB_IMPORT_DIR}")

    # Step 1: Load products catalog
    print(f"\n[1/4] Loading products catalog...")
    products_df = pd.read_csv(PRODUCTS_INPUT)
    print(f"      Loaded {len(products_df)} products")
    print(f"      Columns: {list(products_df.columns)}")

    # Step 2: Extract unique categories
    print(f"\n[2/4] Extracting unique categories...")
    unique_categories = products_df['category'].dropna().unique()
    unique_categories = sorted(unique_categories)

    # Create categories dataframe with IDs starting from 1
    categories_df = pd.DataFrame({
        'category_id': range(1, len(unique_categories) + 1),
        'name': unique_categories
    })

    print(f"      Found {len(categories_df)} unique categories")
    print(f"      Sample categories: {list(categories_df['name'].head(3))}")

    # Save categories.csv
    categories_df.to_csv(CATEGORIES_OUTPUT, index=False)
    print(f"      ✓ Saved: {CATEGORIES_OUTPUT}")

    # Step 3: Map categories to IDs in products
    print(f"\n[3/4] Mapping products to category IDs...")

    # Create mapping dict: category_name -> category_id
    category_map = dict(zip(categories_df['name'], categories_df['category_id']))

    # Map category string to category_id
    products_df['category_id'] = products_df['category'].map(category_map)

    # Verify no NULLs in category_id
    null_categories = products_df[products_df['category_id'].isna()]
    if len(null_categories) > 0:
        print(f"      ⚠ WARNING: {len(null_categories)} products with NULL category_id")
        print(null_categories[['product_id', 'name', 'category']])

    # Reorder columns to match DB schema: product_id, name, specification, category_id, brand
    products_db_df = products_df[['product_id', 'name', 'specification', 'category_id', 'brand']].copy()

    # Convert category_id to int (remove .0)
    products_db_df['category_id'] = products_db_df['category_id'].astype(int)

    print(f"      ✓ Mapped {len(products_db_df)} products to categories")

    # Save products_db.csv
    products_db_df.to_csv(PRODUCTS_OUTPUT, index=False)
    print(f"      ✓ Saved: {PRODUCTS_OUTPUT}")

    # Step 4: Filter prices columns (remove year_month, category)
    print(f"\n[4/4] Filtering prices data...")
    prices_df = pd.read_csv(PRICES_INPUT)
    print(f"      Loaded {len(prices_df):,} price records")
    print(f"      Columns: {list(prices_df.columns)}")

    # Verify all product_ids in prices exist in products
    price_product_ids = set(prices_df['product_id'].unique())
    catalog_product_ids = set(products_db_df['product_id'].unique())
    orphan_ids = price_product_ids - catalog_product_ids

    if orphan_ids:
        print(f"      ⚠ WARNING: {len(orphan_ids)} product_ids in prices not in catalog:")
        print(f"        {sorted(orphan_ids)[:10]}...")
    else:
        print(f"      ✓ All price records reference valid products")

    # Select only the columns needed for DB import (no year_month, no category)
    prices_db_df = prices_df[[
        'product_id', 'date', 'price_min', 'price_max', 'price_avg',
        'price_median', 'price_std', 'store_count', 'offer_count', 'offer_percentage'
    ]].copy()

    print(f"      ✓ Filtered to 10 columns (removed: year_month, category)")

    # Save prices_db.csv
    prices_db_df.to_csv(PRICES_OUTPUT, index=False)
    print(f"      ✓ Saved: {PRICES_OUTPUT}")

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"Categories:  {len(categories_df):>8,} records → {CATEGORIES_OUTPUT.name}")
    print(f"Products:    {len(products_db_df):>8,} records → {PRODUCTS_OUTPUT.name}")
    print(f"Prices:      {len(prices_df):>8,} records → {PRICES_OUTPUT.name}")
    print("\n✓ Ready for PostgreSQL import!")
    print("\nNext steps:")
    print("  1. Import categories first (has no dependencies)")
    print("  2. Import products (depends on categories)")
    print("  3. Import prices (depends on products)")


if __name__ == "__main__":
    main()
