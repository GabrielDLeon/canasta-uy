#!/usr/bin/env python3
"""
Process precios_2025.csv and generate prices_aggregated_2025.csv.
Optimized version using pandas groupby.
"""

import pandas as pd
import numpy as np
from pathlib import Path
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Column names based on source metadata
COLUMN_NAMES = [
    "ID_PrecioDiario",
    "Declaracion",
    "Fecha",
    "FechaAnterior",
    "Oferta",
    "Precio",
    "PrecioAnterior",
    "Publico",
    "Establecimiento",
    "Feria_id",
    "Presentacion_Producto",
]


def process_year_2025():
    """Process year 2025 and generate daily product aggregates."""

    base_dir = Path(__file__).parent.parent
    input_file = base_dir / "data" / "raw" / "precios_2025.csv"
    output_file = base_dir / "data" / "processed" / "prices_aggregated_2025.csv"

    chunk_size = 1000000  # 1M filas por chunk (más grande = más eficiente)

    logger.info("=" * 80)
    logger.info("PROCESSING YEAR 2025 - AGGREGATION BY PRODUCT AND DATE")
    logger.info("=" * 80)
    logger.info(f"Archivo entrada: {input_file}")
    logger.info(f"Archivo salida: {output_file}")
    logger.info(f"Chunk size: {chunk_size:,} filas")

    all_aggregated = []
    total_rows = 0
    chunk_count = 0

    try:
        # Procesar archivo en chunks
        for chunk in pd.read_csv(
            input_file,
            names=COLUMN_NAMES,
            chunksize=chunk_size,
            na_values=["\\N"],
            on_bad_lines="skip",
            low_memory=False,
        ):
            chunk_count += 1

            # Convert data types
            chunk["Precio"] = pd.to_numeric(chunk["Precio"], errors="coerce")
            chunk["Oferta"] = pd.to_numeric(chunk["Oferta"], errors="coerce").fillna(0)
            chunk["Establecimiento"] = pd.to_numeric(
                chunk["Establecimiento"], errors="coerce"
            )
            chunk["Presentacion_Producto"] = pd.to_numeric(
                chunk["Presentacion_Producto"], errors="coerce"
            )

            # Drop rows with invalid data
            chunk = chunk.dropna(subset=["Precio", "Presentacion_Producto", "Fecha"])

            total_rows += len(chunk)

            # Remove duplicates by establishment + product + date (keep last)
            chunk = chunk.sort_values("Declaracion")
            chunk = chunk.drop_duplicates(
                subset=["Establecimiento", "Presentacion_Producto", "Fecha"],
                keep="last",
            )

            # Aggregate by date and product using groupby (highly efficient)
            aggregated = (
                chunk.groupby(["Fecha", "Presentacion_Producto"])
                .agg(
                    price_min=("Precio", "min"),
                    price_max=("Precio", "max"),
                    price_avg=("Precio", "mean"),
                    price_median=("Precio", "median"),
                    price_std=("Precio", "std"),
                    store_count=("Establecimiento", "nunique"),
                    offer_count=("Oferta", "sum"),
                    total_records=("Precio", "count"),
                )
                .reset_index()
            )

            # Calculate offer_percentage
            aggregated["offer_percentage"] = (
                aggregated["offer_count"] / aggregated["total_records"] * 100
            ).round(2)

            # Rename columns
            aggregated = aggregated.rename(
                columns={"Fecha": "date", "Presentacion_Producto": "product_id"}
            )

            # Drop temporary column
            aggregated = aggregated.drop("total_records", axis=1)

            # Round values
            aggregated["price_min"] = aggregated["price_min"].round(2)
            aggregated["price_max"] = aggregated["price_max"].round(2)
            aggregated["price_avg"] = aggregated["price_avg"].round(2)
            aggregated["price_median"] = aggregated["price_median"].round(2)
            aggregated["price_std"] = aggregated["price_std"].fillna(0).round(2)
            aggregated["offer_count"] = aggregated["offer_count"].astype(int)

            all_aggregated.append(aggregated)

            if chunk_count % 5 == 0:
                logger.info(
                    f"  Chunks processed: {chunk_count} | Rows: {total_rows:,} | Aggregated records: {len(aggregated):,}"
                )

        logger.info(
            f"✓ Chunk processing completed: {chunk_count} chunks, {total_rows:,} rows"
        )

        # Combinar todos los resultados
        logger.info("\nCombining results from all chunks...")
        df_combined = pd.concat(all_aggregated, ignore_index=True)

        logger.info(f"  Records before deduplication: {len(df_combined):,}")

        # If a (date, product) appears across chunks, combine them
        # (can happen if chunks split a day)
        df_final = (
            df_combined.groupby(["date", "product_id"])
            .agg(
                price_min=("price_min", "min"),
                price_max=("price_max", "max"),
                price_avg=("price_avg", "mean"),  # Mean of means
                price_median=("price_median", "median"),  # Median of medians
                price_std=("price_std", "mean"),  # Mean of stds
                store_count=("store_count", "sum"),  # Sum of stores
                offer_count=("offer_count", "sum"),  # Sum of offers
                offer_percentage=(
                    "offer_percentage",
                    "mean",
                ),  # Mean of percentages
            )
            .reset_index()
        )

        # Recompute offer_percentage
        # Note: approximate; ideally sum original counts
        df_final["offer_percentage"] = df_final["offer_percentage"].round(2)

        # Sort and reorder columns to match standard format
        df_final = df_final.sort_values(["product_id", "date"]).reset_index(drop=True)

        # Reorder columns: product_id first (like other yearly files)
        column_order = [
            "product_id",
            "date",
            "price_min",
            "price_max",
            "price_avg",
            "price_median",
            "price_std",
            "store_count",
            "offer_count",
            "offer_percentage",
        ]
        df_final = df_final[column_order]

        logger.info(f"  Final records: {len(df_final):,}")

        # Save result
        logger.info(f"\nSaving result: {output_file}")
        df_final.to_csv(output_file, index=False)

        # Show statistics
        logger.info("\n" + "=" * 80)
        logger.info("SUMMARY")
        logger.info("=" * 80)
        logger.info(f"Total records generated: {len(df_final):,}")
        logger.info(f"Unique products: {df_final['product_id'].nunique():,}")
        logger.info(f"Date range: {df_final['date'].min()} to {df_final['date'].max()}")
        logger.info(f"File size: {output_file.stat().st_size / (1024 * 1024):.2f} MB")

        logger.info("\nPrice statistics:")
        logger.info(
            f"  - price_min: ${df_final['price_min'].min():.2f} - ${df_final['price_min'].max():.2f}"
        )
        logger.info(
            f"  - price_max: ${df_final['price_max'].min():.2f} - ${df_final['price_max'].max():.2f}"
        )
        logger.info(
            f"  - price_avg: ${df_final['price_avg'].min():.2f} - ${df_final['price_avg'].max():.2f}"
        )
        logger.info(
            f"  - Establecimientos: {df_final['store_count'].min()} - {df_final['store_count'].max()}"
        )

        logger.info("\n✓ Processing completed successfully")

        return df_final

    except FileNotFoundError:
        logger.error(f"Error: File not found {input_file}")
        raise
    except Exception as e:
        logger.error(f"Error during processing: {e}")
        import traceback

        traceback.print_exc()
        raise


if __name__ == "__main__":
    process_year_2025()
