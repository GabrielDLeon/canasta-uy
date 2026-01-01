#!/usr/bin/env python3
"""
Analyzes differences between product catalogs from different years.
Identifies new, removed, and modified products.
"""

import pandas as pd
import sys

def cargar_catalogo(archivo):
    """Carga un catálogo de productos."""
    try:
        # Intentar con punto y coma primero
        try:
            df = pd.read_csv(archivo, sep=';', encoding='ISO-8859-1')
            if len(df.columns) == 1:  # Si solo hay 1 columna, el separador está mal
                raise ValueError("Separador incorrecto")
        except:
            # Intentar con coma
            df = pd.read_csv(archivo, sep=',', encoding='ISO-8859-1')

        # Limpiar nombres de columnas (remover espacios)
        df.columns = df.columns.str.strip()
        return df
    except Exception as e:
        print(f"❌ Error al cargar {archivo}: {e}")
        return None

def analizar_diferencias(df1, df2, nombre1, nombre2):
    """Analiza diferencias entre dos catálogos."""

    print(f"\n{'=' * 70}")
    print(f"COMPARACIÓN: {nombre1} vs {nombre2}")
    print(f"{'=' * 70}")

    # Columna de ID (puede variar el nombre)
    id_col = df1.columns[0]

    # IDs en cada catálogo
    ids1 = set(df1[id_col])
    ids2 = set(df2[id_col])

    # Productos solo en df1
    solo_en_1 = ids1 - ids2
    # Productos solo en df2
    solo_en_2 = ids2 - ids1
    # Productos en ambos
    en_ambos = ids1 & ids2

    print(f"\n📊 Resumen:")
    print(f"  • Productos en {nombre1}: {len(ids1)}")
    print(f"  • Productos en {nombre2}: {len(ids2)}")
    print(f"  • En ambos: {len(en_ambos)}")
    print(f"  • Solo en {nombre1}: {len(solo_en_1)}")
    print(f"  • Solo en {nombre2}: {len(solo_en_2)}")

    # Mostrar productos solo en df1 (eliminados)
    if solo_en_1:
        print(f"\n❌ Productos ELIMINADOS en {nombre2} (estaban en {nombre1}):")
        eliminados = df1[df1[id_col].isin(solo_en_1)].sort_values(id_col)
        for idx, row in eliminados.head(20).iterrows():
            print(f"  • ID {row[id_col]}: {row.get('nombre', 'N/A')} - {row.get('marca', 'N/A')}")
        if len(solo_en_1) > 20:
            print(f"  ... y {len(solo_en_1) - 20} más")

    # Mostrar productos solo en df2 (nuevos)
    if solo_en_2:
        print(f"\n✅ Productos NUEVOS en {nombre2} (no estaban en {nombre1}):")
        nuevos = df2[df2[id_col].isin(solo_en_2)].sort_values(id_col)
        for idx, row in nuevos.head(20).iterrows():
            print(f"  • ID {row[id_col]}: {row.get('nombre', 'N/A')} - {row.get('marca', 'N/A')}")
        if len(solo_en_2) > 20:
            print(f"  ... y {len(solo_en_2) - 20} más")

    # Verificar cambios en productos existentes (mismo ID, diferente información)
    cambios = []
    for prod_id in en_ambos:
        row1 = df1[df1[id_col] == prod_id].iloc[0]
        row2 = df2[df2[id_col] == prod_id].iloc[0]

        # Comparar todas las columnas
        diferencias = []
        for col in df1.columns:
            if col in df2.columns:
                val1 = str(row1[col]).strip()
                val2 = str(row2[col]).strip()
                if val1 != val2:
                    diferencias.append(f"{col}: '{val1}' → '{val2}'")

        if diferencias:
            cambios.append({
                'id': prod_id,
                'cambios': diferencias,
                'row1': row1,
                'row2': row2
            })

    if cambios:
        print(f"\n🔄 Productos MODIFICADOS (mismo ID, datos diferentes): {len(cambios)}")
        for cambio in cambios[:10]:
            print(f"\n  • ID {cambio['id']}:")
            for dif in cambio['cambios']:
                print(f"    - {dif}")
        if len(cambios) > 10:
            print(f"  ... y {len(cambios) - 10} más productos modificados")
    else:
        print(f"\n✓ No hay productos modificados (datos idénticos para IDs comunes)")

    return {
        'eliminados': solo_en_1,
        'nuevos': solo_en_2,
        'modificados': cambios
    }

def main():
    print("=" * 70)
    print("ANÁLISIS DE CATÁLOGOS DE PRODUCTOS")
    print("=" * 70)

    # Cargar todos los catálogos
    catalogos = {
        '2022': 'data/raw/productos-2022.csv',
        '2023': 'data/raw/productos-2023.csv',
        '2024': 'data/raw/productos-2024.csv',
        '2025': 'data/raw/productos-2025.csv',
    }

    dfs = {}
    for year, path in catalogos.items():
        print(f"\nCargando {year}...", end=" ")
        df = cargar_catalogo(path)
        if df is not None:
            dfs[year] = df
            print(f"✓ {len(df)} productos")
            print(f"  Columnas: {', '.join(df.columns.tolist())}")
        else:
            print("✗ Error")

    if len(dfs) < 2:
        print("\n❌ No hay suficientes catálogos para comparar")
        sys.exit(1)

    # Comparaciones
    years = sorted(dfs.keys())

    # Comparar cada año con el anterior
    print(f"\n{'=' * 70}")
    print("COMPARACIONES AÑO A AÑO")
    print(f"{'=' * 70}")

    resultados = {}
    for i in range(len(years) - 1):
        year1 = years[i]
        year2 = years[i + 1]
        resultado = analizar_diferencias(dfs[year1], dfs[year2], year1, year2)
        resultados[f"{year1}->{year2}"] = resultado

    # Comparar 2022 vs 2025 (extremos)
    if '2022' in dfs and '2025' in dfs:
        print(f"\n{'=' * 70}")
        print("COMPARACIÓN EXTREMOS (2022 vs 2025)")
        print(f"{'=' * 70}")
        analizar_diferencias(dfs['2022'], dfs['2025'], '2022', '2025')

    # Resumen final
    print(f"\n{'=' * 70}")
    print("RESUMEN FINAL")
    print(f"{'=' * 70}")

    print("\nEvolución de productos:")
    for year in years:
        print(f"  • {year}: {len(dfs[year])} productos")

    print("\nCambios detectados:")
    for comparacion, resultado in resultados.items():
        print(f"  • {comparacion}:")
        print(f"    - Nuevos: {len(resultado['nuevos'])}")
        print(f"    - Eliminados: {len(resultado['eliminados'])}")
        print(f"    - Modificados: {len(resultado['modificados'])}")

if __name__ == "__main__":
    main()
