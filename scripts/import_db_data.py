"""
import_db_data.py

Importa los CSVs preparados a PostgreSQL usando COPY (rapido para grandes volumenes).

Input:
- data/processed/db_import/categories.csv
- data/processed/db_import/products_db.csv
- data/processed/db_import/prices_db.csv

Uso:
    uv run python scripts/import_db_data.py

Requiere: psycopg2-binary
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
    """Crea conexion a PostgreSQL."""
    return psycopg2.connect(**DB_CONFIG)


def import_categories(cursor):
    """Importa categorias desde CSV."""
    print("\n[1/3] Importando categorias...")

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
    print(f"      Importadas {count} categorias")


def import_products(cursor):
    """Importa productos desde CSV."""
    print("\n[2/3] Importando productos...")

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
    print(f"      Importados {count} productos")


def import_prices(cursor):
    """Importa precios en batches para no saturar memoria."""
    print("\n[3/3] Importando precios...")

    csv_path = DB_IMPORT_DIR / "prices_db.csv"

    # COPY es mucho mas rapido que INSERT para grandes volumenes
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
    print(f"      Importados {count:,} registros de precios")


def verify_imports(cursor):
    """Verifica que los imports sean correctos."""
    print("\n" + "=" * 60)
    print("VERIFICACION")
    print("=" * 60)

    cursor.execute("SELECT COUNT(*) FROM categories")
    categories = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM products")
    products = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM prices")
    prices = cursor.fetchone()[0]

    cursor.execute("SELECT MIN(date), MAX(date) FROM prices")
    date_range = cursor.fetchone()

    print(f"Categorias:  {categories:>8}")
    print(f"Productos:   {products:>8}")
    print(f"Precios:     {prices:>8,}")
    print(f"Rango fechas: {date_range[0]} a {date_range[1]}")

    # Verificar integridad referencial
    cursor.execute("""
        SELECT COUNT(*) FROM prices p
        WHERE NOT EXISTS (SELECT 1 FROM products pr WHERE pr.product_id = p.product_id)
    """)
    orphan_prices = cursor.fetchone()[0]

    if orphan_prices > 0:
        print(f"\nADVERTENCIA: {orphan_prices:,} precios sin producto asociado")
    else:
        print("\nIntegridad referencial: OK")


def main():
    print("=" * 60)
    print("IMPORT DB DATA - PostgreSQL")
    print("=" * 60)
    print(
        f"\nConectando a: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
    )

    conn = None
    try:
        conn = get_connection()
        cursor = conn.cursor()

        # Truncar tablas antes de importar (limpio)
        print("\nLimpiando tablas existentes...")
        cursor.execute("TRUNCATE TABLE prices, products, categories CASCADE")

        # Importar en orden (respetar FKs)
        import_categories(cursor)
        import_products(cursor)
        import_prices(cursor)

        # Verificar
        verify_imports(cursor)

        # Commit
        conn.commit()
        print("\nImport completado exitosamente!")

    except psycopg2.Error as e:
        print(f"\nError de PostgreSQL: {e}")
        if conn:
            conn.rollback()
        raise
    except Exception as e:
        print(f"\nError inesperado: {e}")
        if conn:
            conn.rollback()
        raise
    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()
