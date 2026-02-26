#!/usr/bin/env python3
"""
Procesa precios_2025.csv completo y genera prices_aggregated_2025.csv
Versión optimizada usando pandas groupby
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

# Nombres de las columnas basados en los metadatos
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
    """Procesa todo el año 2025 y genera archivo agregado por día y producto."""

    base_dir = Path(__file__).parent.parent
    input_file = base_dir / "data" / "raw" / "precios_2025.csv"
    output_file = base_dir / "data" / "processed" / "prices_aggregated_2025.csv"

    chunk_size = 1000000  # 1M filas por chunk (más grande = más eficiente)

    logger.info("=" * 80)
    logger.info("PROCESANDO AÑO 2025 - AGREGACIÓN POR PRODUCTO Y FECHA")
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

            # Convertir tipos de datos
            chunk["Precio"] = pd.to_numeric(chunk["Precio"], errors="coerce")
            chunk["Oferta"] = pd.to_numeric(chunk["Oferta"], errors="coerce").fillna(0)
            chunk["Establecimiento"] = pd.to_numeric(
                chunk["Establecimiento"], errors="coerce"
            )
            chunk["Presentacion_Producto"] = pd.to_numeric(
                chunk["Presentacion_Producto"], errors="coerce"
            )

            # Eliminar filas con datos inválidos
            chunk = chunk.dropna(subset=["Precio", "Presentacion_Producto", "Fecha"])

            total_rows += len(chunk)

            # Eliminar duplicados por Establecimiento + Producto + Fecha (mantener el último)
            chunk = chunk.sort_values("Declaracion")
            chunk = chunk.drop_duplicates(
                subset=["Establecimiento", "Presentacion_Producto", "Fecha"],
                keep="last",
            )

            # Agregar por Fecha y Producto usando groupby (MUY eficiente)
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

            # Calcular offer_percentage
            aggregated["offer_percentage"] = (
                aggregated["offer_count"] / aggregated["total_records"] * 100
            ).round(2)

            # Renombrar columnas
            aggregated = aggregated.rename(
                columns={"Fecha": "date", "Presentacion_Producto": "product_id"}
            )

            # Eliminar columna temporal
            aggregated = aggregated.drop("total_records", axis=1)

            # Redondear valores
            aggregated["price_min"] = aggregated["price_min"].round(2)
            aggregated["price_max"] = aggregated["price_max"].round(2)
            aggregated["price_avg"] = aggregated["price_avg"].round(2)
            aggregated["price_median"] = aggregated["price_median"].round(2)
            aggregated["price_std"] = aggregated["price_std"].fillna(0).round(2)
            aggregated["offer_count"] = aggregated["offer_count"].astype(int)

            all_aggregated.append(aggregated)

            if chunk_count % 5 == 0:
                logger.info(
                    f"  Chunks procesados: {chunk_count} | Filas: {total_rows:,} | Registros agregados: {len(aggregated):,}"
                )

        logger.info(
            f"✓ Procesamiento de chunks completado: {chunk_count} chunks, {total_rows:,} filas"
        )

        # Combinar todos los resultados
        logger.info("\nCombinando resultados de todos los chunks...")
        df_combined = pd.concat(all_aggregated, ignore_index=True)

        logger.info(f"  Registros antes de deduplicar: {len(df_combined):,}")

        # Si un mismo (fecha, producto) aparece en múltiples chunks, combinarlos
        # (puede pasar si los chunks se cortan en medio de un día)
        df_final = (
            df_combined.groupby(["date", "product_id"])
            .agg(
                price_min=("price_min", "min"),
                price_max=("price_max", "max"),
                price_avg=("price_avg", "mean"),  # Promedio de promedios
                price_median=("price_median", "median"),  # Mediana de medianas
                price_std=("price_std", "mean"),  # Promedio de stds
                store_count=("store_count", "sum"),  # Sumar establecimientos
                offer_count=("offer_count", "sum"),  # Sumar ofertas
                offer_percentage=(
                    "offer_percentage",
                    "mean",
                ),  # Promedio de porcentajes
            )
            .reset_index()
        )

        # Recalcular offer_percentage correctamente
        # Nota: esto es aproximado, idealmente deberíamos sumar los conteos originales
        df_final["offer_percentage"] = df_final["offer_percentage"].round(2)

        # Ordenar y reordenar columnas para que coincidan con el formato estándar
        df_final = df_final.sort_values(["product_id", "date"]).reset_index(drop=True)

        # Reordenar columnas: product_id primero (como los demás archivos anuales)
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

        logger.info(f"  Registros finales: {len(df_final):,}")

        # Guardar resultado
        logger.info(f"\nGuardando resultado: {output_file}")
        df_final.to_csv(output_file, index=False)

        # Mostrar estadísticas
        logger.info("\n" + "=" * 80)
        logger.info("RESUMEN")
        logger.info("=" * 80)
        logger.info(f"Total registros generados: {len(df_final):,}")
        logger.info(f"Productos únicos: {df_final['product_id'].nunique():,}")
        logger.info(
            f"Rango de fechas: {df_final['date'].min()} a {df_final['date'].max()}"
        )
        logger.info(
            f"Tamaño archivo: {output_file.stat().st_size / (1024 * 1024):.2f} MB"
        )

        logger.info("\nEstadísticas de precios:")
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

        logger.info("\n✓ Procesamiento completado exitosamente")

        return df_final

    except FileNotFoundError:
        logger.error(f"Error: No se encontró el archivo {input_file}")
        raise
    except Exception as e:
        logger.error(f"Error durante el procesamiento: {e}")
        import traceback

        traceback.print_exc()
        raise


if __name__ == "__main__":
    process_year_2025()
