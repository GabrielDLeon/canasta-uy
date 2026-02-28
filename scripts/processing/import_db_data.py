"""
import_db_data.py

Imports prepared CSVs into PostgreSQL using COPY (fast for large volumes).

Input:
- data/processed/db_import/categories.csv
- data/processed/db_import/products_db.csv
- data/processed/db_import/prices_db.csv

Usage:
    uv run python scripts/import_db_data.py

Requires: psycopg2-binary
    uv add psycopg2-binary
"""

import os
import psycopg2
from pathlib import Path
from psycopg2.extras import execute_values

BASE_DIR = Path(__file__).parent.parent
DB_IMPORT_DIR = BASE_DIR / "data" / "processed" / "db_import"

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": os.getenv("DB_PORT", "5432"),
    "database": os.getenv("DB_NAME", "canastauy"),
    "user": os.getenv("DB_USER", "canastauy_user"),
    "password": os.getenv("DB_PASSWORD", "canastauy_pass"),
}


def get_connection():
    """Create a PostgreSQL connection."""
    return psycopg2.connect(**DB_CONFIG)


def import_categories(cursor):
    """Import categories from CSV."""
    print("\n[1/3] Importing categories...")

    csv_path = DB_IMPORT_DIR / "categories.csv"

    with open(csv_path, "r") as f:
        next(f)  # Skip header
        cursor.copy_expert(
            """
            COPY categories (category_id, name)
            FROM STDIN WITH (FORMAT csv, HEADER false)
            """,
            f,
        )

    cursor.execute("SELECT COUNT(*) FROM categories")
    count = cursor.fetchone()[0]
    print(f"      Imported {count} categories")


def import_products(cursor):
    """Import products from CSV."""
    print("\n[2/3] Importing products...")

    csv_path = DB_IMPORT_DIR / "products_db.csv"

    with open(csv_path, "r") as f:
        next(f)  # Skip header
        cursor.copy_expert(
            """
            COPY products (product_id, name, specification, category_id, brand)
            FROM STDIN WITH (FORMAT csv, HEADER false)
            """,
            f,
        )

    cursor.execute("SELECT COUNT(*) FROM products")
    count = cursor.fetchone()[0]
    print(f"      Imported {count} products")


def import_prices(cursor):
    """Import prices using COPY for large volumes."""
    print("\n[3/3] Importing prices...")

    csv_path = DB_IMPORT_DIR / "prices_db.csv"

    # COPY is much faster than INSERT for large volumes
    with open(csv_path, "r") as f:
        next(f)  # Skip header
        cursor.copy_expert(
            """
            COPY prices (
                product_id, date, price_min, price_max, price_avg,
                price_median, price_std, store_count, offer_count, offer_percentage
            )
            FROM STDIN WITH (FORMAT csv, HEADER false)
            """,
            f,
        )

    cursor.execute("SELECT COUNT(*) FROM prices")
    count = cursor.fetchone()[0]
    print(f"      Imported {count:,} price records")


def verify_imports(cursor):
    """Verify that imports are correct."""
    print("\n" + "=" * 60)
    print("VERIFICATION")
    print("=" * 60)

    cursor.execute("SELECT COUNT(*) FROM categories")
    categories = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM products")
    products = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM prices")
    prices = cursor.fetchone()[0]

    cursor.execute("SELECT MIN(date), MAX(date) FROM prices")
    date_range = cursor.fetchone()

    print(f"Categories:  {categories:>8}")
    print(f"Products:    {products:>8}")
    print(f"Prices:      {prices:>8,}")
    print(f"Date range: {date_range[0]} to {date_range[1]}")

    # Verify referential integrity
    cursor.execute("""
        SELECT COUNT(*) FROM prices p
        WHERE NOT EXISTS (SELECT 1 FROM products pr WHERE pr.product_id = p.product_id)
    """)
    orphan_prices = cursor.fetchone()[0]

    if orphan_prices > 0:
        print(f"\nWARNING: {orphan_prices:,} prices without a matching product")
    else:
        print("\nReferential integrity: OK")


def main():
    print("=" * 60)
    print("IMPORT DB DATA - PostgreSQL")
    print("=" * 60)
    print(
        f"\nConnecting to: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
    )

    conn = None
    try:
        conn = get_connection()
        cursor = conn.cursor()

        # Truncate tables before importing (clean)
        print("\nTruncating existing tables...")
        cursor.execute("TRUNCATE TABLE prices, products, categories CASCADE")

        # Import in order (respect FKs)
        import_categories(cursor)
        import_products(cursor)
        import_prices(cursor)

        # Verify
        verify_imports(cursor)

        # Commit
        conn.commit()
        print("\nImport completed successfully!")

    except psycopg2.Error as e:
        print(f"\nPostgreSQL error: {e}")
        if conn:
            conn.rollback()
        raise
    except Exception as e:
        print(f"\nUnexpected error: {e}")
        if conn:
            conn.rollback()
        raise
    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()
