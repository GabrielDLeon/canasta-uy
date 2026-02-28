#!/usr/bin/env python3
"""
Consolidates product catalogs from all years into a single normalized file.
Applies best practices: UTF-8, headers, consistent format.
"""

import pandas as pd
import sys


def detect_separator(file_path):
    """Detect whether the file uses comma or semicolon."""
    with open(file_path, "rb") as f:
        first_line = f.readline().decode("ISO-8859-1")
        return ";" if ";" in first_line else ","


def load_catalog(file_path):
    """Load catalog using the correct encoding and separator."""
    try:
        sep = detect_separator(file_path)
        df = pd.read_csv(file_path, sep=sep, encoding="ISO-8859-1")
        df.columns = df.columns.str.strip()
        return df
    except Exception as e:
        print(f"Error loading {file_path}: {e}")
        return None


def main():
    print("=" * 70)
    print("PRODUCT CATALOG CONSOLIDATION")
    print("=" * 70)

    # Use productos-2025 which contains all items (368 original + 11 new)
    base_file = "data/raw/productos-2025.csv"
    output_file = "data/processed/products_catalog.csv"

    print(f"\nLoading full catalog: {base_file}")
    df = load_catalog(base_file)

    if df is None:
        sys.exit(1)

    print(f"Loaded {len(df)} products")

    # Normalize columns to snake_case
    df.rename(
        columns={
            "id.producto": "product_id",
            "producto": "category",
            "marca": "brand",
            "especificacion": "specification",
            "nombre": "name",
        },
        inplace=True,
    )

    # Check for duplicate IDs
    duplicates = df["product_id"].duplicated().sum()
    if duplicates > 0:
        print(f"Warning: {duplicates} duplicate IDs found")

    # Sort by ID
    df.sort_values("product_id", inplace=True)

    # Save normalized: UTF-8, comma, headers
    print(f"\nSaving consolidated file: {output_file}")
    df.to_csv(output_file, index=False, encoding="utf-8")

    print(f"\n{'=' * 70}")
    print("CONSOLIDATION COMPLETE")
    print(f"{'=' * 70}")
    print(f"File: {output_file}")
    print(f"Products: {len(df)}")
    print("Encoding: UTF-8")
    print("Separator: comma (,)")
    print("Headers: Yes")
    print(f"\nColumns: {', '.join(df.columns.tolist())}")

    print(f"\n{'=' * 70}")
    print("DATA SAMPLE")
    print(f"{'=' * 70}")
    print(df.head(10).to_string(index=False))

    print(f"\n{'=' * 70}")
    print("NEW PRODUCTS 2025 (ID >= 371)")
    print(f"{'=' * 70}")
    new_products = df[df["product_id"] >= 371]
    print(new_products.to_string(index=False))


if __name__ == "__main__":
    main()
