#!/usr/bin/env python3
"""
Script para agregar precios por producto con estadísticas (min, max, promedio, mediana, etc.)
Reduce significativamente el volumen de datos manteniendo información valiosa.
"""

import pandas as pd
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


def agregar_por_producto(
    input_file, output_file, fecha_filtro="2025-01-01", chunk_size=100000
):
    """
    Agrega precios por producto calculando estadísticas.

    Para cada producto en la fecha especificada, calcula:
    - Precio mínimo, máximo, promedio, mediana
    - Desviación estándar
    - Cantidad de establecimientos que lo venden
    - Cantidad y porcentaje de ofertas

    Args:
        input_file: Ruta del archivo de entrada
        output_file: Ruta del archivo de salida
        fecha_filtro: Fecha a filtrar (formato YYYY-MM-DD)
        chunk_size: Tamaño de cada chunk para procesar
    """
    print(f"Iniciando agregación de {input_file}...")
    print(f"Filtrando registros con Fecha = {fecha_filtro}")
    print(f"Agrupando por Producto con estadísticas")
    print(f"Procesando en chunks de {chunk_size:,} filas\n")

    total_rows_processed = 0
    total_rows_filtered = 0

    # Almacenar todos los datos filtrados
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
        # Convertir tipos de datos
        chunk["Precio"] = pd.to_numeric(chunk["Precio"], errors="coerce")
        chunk["PrecioAnterior"] = pd.to_numeric(
            chunk["PrecioAnterior"], errors="coerce"
        )
        chunk["Oferta"] = (
            pd.to_numeric(chunk["Oferta"], errors="coerce").fillna(0).astype(int)
        )
        chunk["Establecimiento"] = pd.to_numeric(
            chunk["Establecimiento"], errors="coerce"
        ).astype("Int64")
        chunk["Presentacion_Producto"] = pd.to_numeric(
            chunk["Presentacion_Producto"], errors="coerce"
        ).astype("Int64")

        # Filtrar por fecha
        filtered_chunk = chunk[chunk["Fecha"] == fecha_filtro]

        # Eliminar filas con precio inválido
        filtered_chunk = filtered_chunk[filtered_chunk["Precio"].notna()]

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

    # Combinar y agregar
    if filtered_data:
        print("\nCombinando datos y calculando estadísticas por producto...")
        df = pd.concat(filtered_data, ignore_index=True)

        print(f"Total de registros antes de agregar: {len(df):,}")

        # Eliminar duplicados de Establecimiento + Producto (mantener el último)
        df = df.sort_values("Declaracion")
        df_dedup = df.drop_duplicates(
            subset=["Establecimiento", "Presentacion_Producto"], keep="last"
        )

        print(f"Registros únicos después de deduplicar: {len(df_dedup):,}")

        # Agregar por producto
        agregado = (
            df_dedup.groupby("Presentacion_Producto")
            .agg(
                Fecha=("Fecha", "first"),
                Precio_Min=("Precio", "min"),
                Precio_Max=("Precio", "max"),
                Precio_Promedio=("Precio", "mean"),
                Precio_Mediana=("Precio", "median"),
                Precio_Desviacion=("Precio", "std"),
                Cantidad_Establecimientos=("Establecimiento", "count"),
                Cantidad_Ofertas=("Oferta", "sum"),
            )
            .reset_index()
        )

        # Calcular porcentaje de ofertas
        agregado["Porcentaje_Ofertas"] = (
            agregado["Cantidad_Ofertas"] / agregado["Cantidad_Establecimientos"] * 100
        ).round(2)

        # Renombrar columna de producto para mayor claridad
        agregado.rename(columns={"Presentacion_Producto": "Producto_ID"}, inplace=True)

        # Redondear valores numéricos
        agregado["Precio_Promedio"] = agregado["Precio_Promedio"].round(2)
        agregado["Precio_Mediana"] = agregado["Precio_Mediana"].round(2)
        agregado["Precio_Desviacion"] = agregado["Precio_Desviacion"].round(2)

        # Reordenar columnas
        columnas_orden = [
            "Producto_ID",
            "Fecha",
            "Precio_Min",
            "Precio_Max",
            "Precio_Promedio",
            "Precio_Mediana",
            "Precio_Desviacion",
            "Cantidad_Establecimientos",
            "Cantidad_Ofertas",
            "Porcentaje_Ofertas",
        ]
        agregado = agregado[columnas_orden]

        # Guardar resultado
        agregado.to_csv(output_file, index=False)

        print(f"\n✓ Proceso completado!")
        print(f"Total procesado: {total_rows_processed:,} filas")
        print(f"Registros originales filtrados: {len(df):,}")
        print(f"Registros únicos (deduplicados): {len(df_dedup):,}")
        print(f"Productos únicos (agregados): {len(agregado):,}")
        print(
            f"Reducción: {len(df):,} → {len(agregado):,} registros "
            f"({(1 - len(agregado) / len(df)) * 100:.1f}% menos)"
        )
        print(f"Archivo guardado en: {output_file}")

        return agregado
    else:
        print(f"\n⚠ No se encontraron registros con fecha {fecha_filtro}")
        return None


def main():
    input_file = "data/raw/precios_2025.csv"
    output_file = "data/processed/precios_agregados_2025-01-01.csv"
    fecha_filtro = "2025-01-01"

    # Permitir especificar fecha como argumento
    if len(sys.argv) > 1:
        fecha_filtro = sys.argv[1]
        output_file = f"data/processed/precios_agregados_{fecha_filtro}.csv"

    try:
        resultado = agregar_por_producto(input_file, output_file, fecha_filtro)

        # Mostrar estadísticas del resultado
        if resultado is not None and len(resultado) > 0:
            print("\n" + "=" * 60)
            print("ESTADÍSTICAS DEL ARCHIVO AGREGADO")
            print("=" * 60)
            print(f"\nProductos totales: {len(resultado):,}")
            print(f"\nRango de precios:")
            print(f"  - Precio más bajo: ${resultado['Precio_Min'].min():.2f}")
            print(f"  - Precio más alto: ${resultado['Precio_Max'].max():.2f}")
            print(f"\nEstadísticas de precios promedios:")
            print(f"  - Promedio general: ${resultado['Precio_Promedio'].mean():.2f}")
            print(f"  - Mediana general: ${resultado['Precio_Promedio'].median():.2f}")
            print(f"\nEstablecimientos:")
            print(
                f"  - Promedio por producto: {resultado['Cantidad_Establecimientos'].mean():.1f}"
            )
            print(f"  - Máximo: {resultado['Cantidad_Establecimientos'].max()}")
            print(f"  - Mínimo: {resultado['Cantidad_Establecimientos'].min()}")
            print(f"\nOfertas:")
            total_ofertas = resultado["Cantidad_Ofertas"].sum()
            total_registros = resultado["Cantidad_Establecimientos"].sum()
            print(f"  - Total de ofertas: {int(total_ofertas):,}")
            print(
                f"  - Porcentaje global: {(total_ofertas / total_registros * 100):.1f}%"
            )
            print(
                f"  - Productos con >50% de ofertas: "
                f"{len(resultado[resultado['Porcentaje_Ofertas'] > 50])}"
            )

            # Mostrar algunos ejemplos
            print(f"\n" + "=" * 60)
            print("MUESTRA DE DATOS (primeros 5 productos)")
            print("=" * 60)
            print(resultado.head().to_string(index=False))

    except FileNotFoundError:
        print(f"Error: No se encontró el archivo {input_file}")
        sys.exit(1)
    except Exception as e:
        print(f"Error durante el procesamiento: {e}")
        import traceback

        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
