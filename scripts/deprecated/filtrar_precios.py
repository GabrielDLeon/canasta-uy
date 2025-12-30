#!/usr/bin/env python3
"""
Script para filtrar el archivo precios_2025.csv y extraer solo los registros
de una fecha específica (2025-01-01 por defecto).
"""

import pandas as pd
from datetime import datetime
import sys

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


def filtrar_por_fecha(
    input_file, output_file, fecha_filtro="2025-01-01", chunk_size=100000
):
    """
    Filtra un archivo CSV grande por fecha usando procesamiento por chunks.
    Elimina duplicados manteniendo solo un registro por combinación de
    Establecimiento + Producto por día.

    Args:
        input_file: Ruta del archivo de entrada
        output_file: Ruta del archivo de salida
        fecha_filtro: Fecha a filtrar (formato YYYY-MM-DD)
        chunk_size: Tamaño de cada chunk para procesar
    """
    print(f"Iniciando filtrado de {input_file}...")
    print(f"Filtrando registros con Fecha = {fecha_filtro}")
    print(f"Eliminando duplicados por Establecimiento + Producto")
    print(f"Procesando en chunks de {chunk_size:,} filas\n")

    total_rows_processed = 0
    total_rows_filtered = 0
    total_duplicates_removed = 0

    # Almacenar todos los datos filtrados en memoria antes de deduplicar
    filtered_data = []

    # Procesar el archivo en chunks
    for i, chunk in enumerate(
        pd.read_csv(
            input_file,
            names=COLUMN_NAMES,
            chunksize=chunk_size,
            na_values=["\\N"],
            on_bad_lines="skip",
            low_memory=False,
        )
    ):
        # Filtrar por fecha
        filtered_chunk = chunk[chunk["Fecha"] == fecha_filtro]

        total_rows_processed += len(chunk)
        total_rows_filtered += len(filtered_chunk)

        # Acumular datos filtrados
        if len(filtered_chunk) > 0:
            filtered_data.append(filtered_chunk)

        # Mostrar progreso
        if (i + 1) % 10 == 0:
            print(
                f"Procesados: {total_rows_processed:,} filas | "
                f"Filtrados: {total_rows_filtered:,} registros"
            )

    # Combinar todos los chunks filtrados
    if filtered_data:
        print("\nCombinando datos y eliminando duplicados...")
        df_final = pd.concat(filtered_data, ignore_index=True)

        # Contar antes de eliminar duplicados
        antes_dedup = len(df_final)

        # Eliminar duplicados manteniendo el último registro (más reciente por Declaracion)
        # Ordenar por Declaracion para que drop_duplicates mantenga el más reciente
        df_final = df_final.sort_values("Declaracion")
        df_final = df_final.drop_duplicates(
            subset=["Establecimiento", "Presentacion_Producto"], keep="last"
        )

        despues_dedup = len(df_final)
        total_duplicates_removed = antes_dedup - despues_dedup

        # Guardar resultado
        df_final.to_csv(output_file, index=False)

        print(f"\n✓ Proceso completado!")
        print(f"Total procesado: {total_rows_processed:,} filas")
        print(f"Total filtrado: {antes_dedup:,} registros")
        print(f"Duplicados eliminados: {total_duplicates_removed:,} registros")
        print(f"Registros únicos: {despues_dedup:,} registros")
        print(f"Archivo guardado en: {output_file}")

        return despues_dedup
    else:
        print(f"\n⚠ No se encontraron registros con fecha {fecha_filtro}")
        return 0


def main():
    input_file = "data/raw/precios_2025.csv"
    output_file = "data/processed/precios_2025-01-01.csv"
    fecha_filtro = "2025-01-01"

    # Permitir especificar fecha como argumento
    if len(sys.argv) > 1:
        fecha_filtro = sys.argv[1]
        output_file = f"data/processed/precios_{fecha_filtro}.csv"

    try:
        total = filtrar_por_fecha(input_file, output_file, fecha_filtro)

        # Mostrar algunas estadísticas básicas si hay datos
        if total > 0:
            print("\nEstadísticas básicas del archivo filtrado:")
            df = pd.read_csv(output_file)
            print(f"  - Registros: {len(df):,}")
            print(f"  - Establecimientos únicos: {df['Establecimiento'].nunique():,}")
            print(f"  - Productos únicos: {df['Presentacion_Producto'].nunique():,}")
            print(f"  - Precio promedio: ${df['Precio'].mean():.2f}")
            print(
                f"  - Registros en oferta: {df['Oferta'].sum():,} ({df['Oferta'].sum() / len(df) * 100:.1f}%)"
            )

    except FileNotFoundError:
        print(f"Error: No se encontró el archivo {input_file}")
        sys.exit(1)
    except Exception as e:
        print(f"Error durante el procesamiento: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
