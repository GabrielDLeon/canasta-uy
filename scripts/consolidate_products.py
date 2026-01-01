#!/usr/bin/env python3
"""
Consolidates product catalogs from all years into a single normalized file.
Applies best practices: UTF-8, headers, consistent format.
"""

import pandas as pd
import sys


def detectar_separador(archivo):
    """Detecta si el archivo usa coma o punto y coma."""
    with open(archivo, 'rb') as f:
        primera_linea = f.readline().decode('ISO-8859-1')
        return ';' if ';' in primera_linea else ','


def cargar_catalogo(archivo):
    """Carga catálogo con encoding y separador correctos."""
    try:
        sep = detectar_separador(archivo)
        df = pd.read_csv(archivo, sep=sep, encoding='ISO-8859-1')
        df.columns = df.columns.str.strip()
        return df
    except Exception as e:
        print(f"❌ Error al cargar {archivo}: {e}")
        return None


def main():
    print("=" * 70)
    print("CONSOLIDACIÓN DE CATÁLOGOS DE PRODUCTOS")
    print("=" * 70)

    # Usar productos-2025 que contiene todos (368 originales + 11 nuevos)
    archivo_base = 'data/raw/productos-2025.csv'
    archivo_salida = 'data/processed/products_catalog.csv'

    print(f"\nCargando catálogo completo: {archivo_base}")
    df = cargar_catalogo(archivo_base)

    if df is None:
        sys.exit(1)

    print(f"✓ Cargados {len(df)} productos")

    # Normalizar columnas a snake_case
    df.rename(columns={
        'id.producto': 'product_id',
        'producto': 'category',
        'marca': 'brand',
        'especificacion': 'specification',
        'nombre': 'name'
    }, inplace=True)

    # Validar que no haya duplicados
    duplicados = df['product_id'].duplicated().sum()
    if duplicados > 0:
        print(f"⚠ Advertencia: {duplicados} IDs duplicados encontrados")

    # Ordenar por ID
    df.sort_values('product_id', inplace=True)

    # Guardar normalizado: UTF-8, coma, headers
    print(f"\nGuardando consolidado: {archivo_salida}")
    df.to_csv(archivo_salida, index=False, encoding='utf-8')

    print(f"\n{'=' * 70}")
    print("✓ CONSOLIDACIÓN COMPLETADA")
    print(f"{'=' * 70}")
    print(f"Archivo: {archivo_salida}")
    print(f"Productos: {len(df)}")
    print(f"Encoding: UTF-8")
    print(f"Separador: coma (,)")
    print(f"Headers: Sí")
    print(f"\nColumnas: {', '.join(df.columns.tolist())}")

    print(f"\n{'=' * 70}")
    print("MUESTRA DE DATOS")
    print(f"{'=' * 70}")
    print(df.head(10).to_string(index=False))

    print(f"\n{'=' * 70}")
    print("PRODUCTOS NUEVOS 2025 (ID >= 371)")
    print(f"{'=' * 70}")
    nuevos = df[df['product_id'] >= 371]
    print(nuevos.to_string(index=False))


if __name__ == "__main__":
    main()
