#!/usr/bin/env python3
"""
Procesa datasets de precios 2016-2017 (schema antiguo de 5 columnas sin header).
Genera agregaciones por producto por día con mismo formato de salida que schema nuevo.

Uso:
    python scripts/process_prices_old_schema.py -i data/raw/precios_2016.csv -o data/processed/prices_aggregated_2016.csv
"""

import pandas as pd
import sys
import argparse
from datetime import datetime

# Schema antiguo (2016-2017): 5 columnas, SIN HEADER
COLUMN_NAMES = [
    "Establecimiento",
    "Presentacion_Producto",
    "Precio",
    "Fecha",
    "Oferta",
]


def procesar_dataset_old_schema(input_file, output_file, chunk_size=100000):
    """
    Procesa dataset con schema antiguo (2016-2017) agregando por producto y fecha.
    """
    print(f"=" * 70)
    print(f"PROCESAMIENTO DATASET SCHEMA ANTIGUO (2016-2017)")
    print(f"=" * 70)
    print(f"Archivo de entrada: {input_file}")
    print(f"Archivo de salida: {output_file}")
    print(f"Tamaño de chunk: {chunk_size:,} filas\n")

    inicio = datetime.now()

    total_rows_processed = 0
    total_rows_valid = 0
    total_rows_invalid = 0

    all_data = []

    print("Iniciando procesamiento por chunks...\n")

    for i, chunk in enumerate(
        pd.read_csv(
            input_file,
            names=COLUMN_NAMES,
            header=None,
            chunksize=chunk_size,
            na_values=["\\N"],
            on_bad_lines="skip",
            low_memory=False,
        ),
        1,
    ):
        # Convertir tipos
        chunk["Precio"] = pd.to_numeric(chunk["Precio"], errors="coerce")
        chunk["Oferta"] = (
            pd.to_numeric(chunk["Oferta"], errors="coerce").fillna(0).astype(int)
        )
        chunk["Establecimiento"] = pd.to_numeric(
            chunk["Establecimiento"], errors="coerce"
        ).astype("Int64")
        chunk["Presentacion_Producto"] = pd.to_numeric(
            chunk["Presentacion_Producto"], errors="coerce"
        ).astype("Int64")

        # Filtrar filas inválidas y precios anómalos (>$10,000 probablemente errores)
        valid_rows = chunk[
            chunk["Precio"].notna()
            & chunk["Presentacion_Producto"].notna()
            & chunk["Establecimiento"].notna()
            & (chunk["Precio"] > 0)
            & (chunk["Precio"] <= 10000)
        ].copy()

        invalid_count = len(chunk) - len(valid_rows)

        total_rows_processed += len(chunk)
        total_rows_valid += len(valid_rows)
        total_rows_invalid += invalid_count

        if len(valid_rows) > 0:
            all_data.append(
                valid_rows[
                    [
                        "Fecha",
                        "Presentacion_Producto",
                        "Precio",
                        "Oferta",
                        "Establecimiento",
                    ]
                ]
            )

        if i % 10 == 0:
            print(
                f"Chunk {i:>4}: Procesadas {total_rows_processed:>12,} filas | "
                f"Válidas: {total_rows_valid:>12,} | "
                f"Inválidas: {total_rows_invalid:>8,}"
            )

    print(f"\n{'=' * 70}")
    print("Fase 1: Lectura completada")
    print(f"{'=' * 70}")
    print(f"Total procesado: {total_rows_processed:,} filas")
    print(
        f"Total válido: {total_rows_valid:,} filas ({total_rows_valid / total_rows_processed * 100:.1f}%)"
    )
    print(
        f"Total inválido: {total_rows_invalid:,} filas ({total_rows_invalid / total_rows_processed * 100:.1f}%)"
    )

    if not all_data:
        print("\n⚠ No se encontraron datos válidos para procesar")
        return None

    print(f"\n{'=' * 70}")
    print("Fase 2: Combinando chunks...")
    print(f"{'=' * 70}")

    df = pd.concat(all_data, ignore_index=True)
    print(f"DataFrame combinado: {len(df):,} registros")

    # Deduplicación: Establecimiento + Producto + Fecha (sin Declaracion en schema antiguo)
    print(f"\n{'=' * 70}")
    print("Fase 3: Eliminando duplicados...")
    print(f"{'=' * 70}")

    antes_dedup = len(df)
    df = df.drop_duplicates(
        subset=["Fecha", "Establecimiento", "Presentacion_Producto"], keep="last"
    )
    despues_dedup = len(df)
    duplicados = antes_dedup - despues_dedup

    print(f"Registros antes: {antes_dedup:,}")
    print(f"Registros después: {despues_dedup:,}")
    print(
        f"Duplicados eliminados: {duplicados:,} ({duplicados / antes_dedup * 100:.1f}%)"
    )

    # Agregar por producto y fecha
    print(f"\n{'=' * 70}")
    print("Fase 4: Agregando por producto y fecha...")
    print(f"{'=' * 70}")

    agregado = (
        df.groupby(["Presentacion_Producto", "Fecha"])
        .agg(
            price_min=("Precio", "min"),
            price_max=("Precio", "max"),
            price_avg=("Precio", "mean"),
            price_median=("Precio", "median"),
            price_std=("Precio", "std"),
            store_count=("Establecimiento", "count"),
            offer_count=("Oferta", "sum"),
        )
        .reset_index()
    )

    agregado["offer_percentage"] = (
        agregado["offer_count"] / agregado["store_count"] * 100
    ).round(2)

    agregado.rename(
        columns={"Presentacion_Producto": "product_id", "Fecha": "date"}, inplace=True
    )

    agregado["price_avg"] = agregado["price_avg"].round(2)
    agregado["price_median"] = agregado["price_median"].round(2)
    agregado["price_std"] = agregado["price_std"].round(2)
    agregado["price_std"] = agregado["price_std"].fillna(0)
    agregado["offer_count"] = agregado["offer_count"].astype(int)

    columnas_orden = [
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
    agregado = agregado[columnas_orden]
    agregado = agregado.sort_values(["date", "product_id"])

    print(f"Registros agregados: {len(agregado):,}")
    print(f"Productos únicos: {agregado['product_id'].nunique():,}")
    print(f"Fechas únicas: {agregado['date'].nunique():,}")

    # Guardar
    print(f"\n{'=' * 70}")
    print("Fase 5: Guardando archivo...")
    print(f"{'=' * 70}")

    agregado.to_csv(output_file, index=False)

    fin = datetime.now()
    duracion = (fin - inicio).total_seconds()

    print(f"\n{'=' * 70}")
    print("✓ PROCESO COMPLETADO EXITOSAMENTE")
    print(f"{'=' * 70}")
    print(f"Archivo guardado: {output_file}")
    print(
        f"Tiempo de procesamiento: {duracion:.1f} segundos ({duracion / 60:.1f} minutos)"
    )
    print(f"Reducción de datos: {antes_dedup:,} → {len(agregado):,} registros")
    print(f"Factor de compresión: {antes_dedup / len(agregado):.1f}x")

    return agregado


def mostrar_estadisticas(df):
    """Muestra estadísticas descriptivas del dataset agregado."""

    print(f"\n{'=' * 70}")
    print("ESTADÍSTICAS DEL DATASET AGREGADO")
    print(f"{'=' * 70}")

    print(f"\n📊 Resumen general:")
    print(f"  • Total de registros: {len(df):,}")
    print(f"  • Productos únicos: {df['product_id'].nunique():,}")
    print(f"  • Fechas únicas: {df['date'].nunique():,}")
    print(f"  • Rango de fechas: {df['date'].min()} a {df['date'].max()}")

    print(f"\n💰 Estadísticas de precios:")
    print(f"  • Precio mínimo global: ${df['price_min'].min():.2f}")
    print(f"  • Precio máximo global: ${df['price_max'].max():.2f}")
    print(f"  • Precio promedio global: ${df['price_avg'].mean():.2f}")
    print(f"  • Precio mediano global: ${df['price_median'].median():.2f}")

    print(f"\n🏪 Estadísticas de establecimientos:")
    print(f"  • Promedio por producto/día: {df['store_count'].mean():.1f}")
    print(f"  • Máximo: {df['store_count'].max()}")
    print(f"  • Mínimo: {df['store_count'].min()}")

    print(f"\n🏷️  Estadísticas de ofertas:")
    total_ofertas = df["offer_count"].sum()
    total_registros = df["store_count"].sum()
    print(f"  • Total de ofertas: {int(total_ofertas):,}")
    print(f"  • Porcentaje global: {(total_ofertas / total_registros * 100):.1f}%")

    print(f"\n📈 Top 5 productos por frecuencia:")
    top_productos = df["product_id"].value_counts().head(5)
    for product_id, count in top_productos.items():
        print(f"  • Producto {product_id}: {count} días")

    print(f"\n{'=' * 70}")
    print("MUESTRA DE DATOS (primeros 10 registros)")
    print(f"{'=' * 70}")
    print(df.head(10).to_string(index=False))

    print(f"\n{'=' * 70}")


def main():
    parser = argparse.ArgumentParser(
        description="Procesa datasets de precios 2016-2017 (schema antiguo)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Ejemplos de uso:
  python scripts/process_prices_old_schema.py -i data/raw/precios_2016.csv -o data/processed/prices_aggregated_2016.csv
  python scripts/process_prices_old_schema.py -i data/raw/precios_2017.csv -o data/processed/prices_aggregated_2017.csv
        """,
    )

    parser.add_argument(
        "-i", "--input",
        required=True,
        help="Ruta del archivo CSV de entrada (precios_2016.csv o precios_2017.csv)",
    )

    parser.add_argument(
        "-o", "--output",
        required=True,
        help="Ruta del archivo CSV de salida",
    )

    parser.add_argument(
        "--chunk-size",
        type=int,
        default=100000,
        help="Tamaño de chunk para procesamiento (default: 100000)",
    )

    args = parser.parse_args()

    try:
        resultado = procesar_dataset_old_schema(args.input, args.output, args.chunk_size)

        if resultado is not None:
            mostrar_estadisticas(resultado)

            print(f"\n✅ Archivo listo para importar en base de datos")
            print(f"   Columnas: {', '.join(resultado.columns.tolist())}")

    except FileNotFoundError:
        print(f"\n❌ Error: No se encontró el archivo {args.input}")
        print(f"   Asegúrate de que el archivo existe en la ubicación correcta.")
        sys.exit(1)
    except MemoryError:
        print(f"\n❌ Error: Memoria insuficiente para procesar el archivo completo")
        print(
            f"   Intenta aumentar el tamaño del chunk_size con --chunk-size"
        )
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error durante el procesamiento: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
