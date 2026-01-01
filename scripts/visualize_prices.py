#!/usr/bin/env python3
"""
Generates visualizations of product price evolution over time.
"""

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from datetime import datetime
import sys
import os


def cargar_datos(archivo_precios, archivo_productos):
    """Carga datos de precios y productos."""
    print(f"Cargando datos...")

    precios = pd.read_csv(archivo_precios)
    precios['date'] = pd.to_datetime(precios['date'])

    productos = pd.read_csv(archivo_productos)

    print(f"✓ {len(precios):,} registros de precios")
    print(f"✓ {len(productos)} productos")

    return precios, productos


def graficar_producto(precios_producto, nombre_producto, output_dir):
    """Genera gráfica de evolución de precio para un producto."""

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10))
    fig.suptitle(f'Evolución de Precios - {nombre_producto}', fontsize=16, fontweight='bold')

    dates = precios_producto['date']

    # Gráfica 1: Rango de precios (min, median, max)
    ax1.fill_between(dates, precios_producto['price_min'], precios_producto['price_max'],
                     alpha=0.2, color='gray', label='Rango (min-max)')
    ax1.plot(dates, precios_producto['price_median'], 'b-', linewidth=2, label='Mediana')
    ax1.plot(dates, precios_producto['price_avg'], 'g--', linewidth=1.5, label='Promedio')

    ax1.set_ylabel('Precio ($)', fontsize=12)
    ax1.set_title('Rango y Tendencia de Precios', fontsize=14)
    ax1.legend(loc='best')
    ax1.grid(True, alpha=0.3)
    ax1.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m'))
    ax1.xaxis.set_major_locator(mdates.MonthLocator(interval=1))
    plt.setp(ax1.xaxis.get_majorticklabels(), rotation=45, ha='right')

    # Gráfica 2: Volatilidad (std) y cobertura (store_count)
    ax2_twin = ax2.twinx()

    ax2.plot(dates, precios_producto['price_std'], 'r-', linewidth=2, label='Desv. Estándar')
    ax2.set_ylabel('Volatilidad (std)', fontsize=12, color='r')
    ax2.tick_params(axis='y', labelcolor='r')
    ax2.set_title('Volatilidad y Disponibilidad', fontsize=14)
    ax2.grid(True, alpha=0.3)

    ax2_twin.plot(dates, precios_producto['store_count'], 'orange', linewidth=2, label='Establecimientos')
    ax2_twin.set_ylabel('Cantidad de Establecimientos', fontsize=12, color='orange')
    ax2_twin.tick_params(axis='y', labelcolor='orange')

    ax2.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m'))
    ax2.xaxis.set_major_locator(mdates.MonthLocator(interval=1))
    plt.setp(ax2.xaxis.get_majorticklabels(), rotation=45, ha='right')

    ax2.legend(loc='upper left')
    ax2_twin.legend(loc='upper right')

    plt.tight_layout()

    # Crear directorio si no existe
    os.makedirs(output_dir, exist_ok=True)

    # Sanitizar nombre para archivo
    nombre_archivo = nombre_producto.replace('/', '-').replace(' ', '_')[:50]
    output_path = f"{output_dir}/{nombre_archivo}.png"

    plt.savefig(output_path, dpi=150, bbox_inches='tight')
    plt.close()

    return output_path


def calcular_estadisticas(precios_producto):
    """Calcula estadísticas del período."""
    stats = {
        'precio_min_historico': precios_producto['price_min'].min(),
        'precio_max_historico': precios_producto['price_max'].max(),
        'precio_median_promedio': precios_producto['price_median'].mean(),
        'variacion_porcentual': ((precios_producto['price_median'].iloc[-1] /
                                 precios_producto['price_median'].iloc[0]) - 1) * 100,
        'volatilidad_promedio': precios_producto['price_std'].mean(),
        'dias_con_datos': len(precios_producto),
        'establecimientos_promedio': precios_producto['store_count'].mean(),
    }
    return stats


def main():
    if len(sys.argv) < 2:
        print("Uso: python visualizar_precios.py <product_id> [year]")
        print("\nEjemplos:")
        print("  python visualizar_precios.py 1          # Producto 1, todos los años")
        print("  python visualizar_precios.py 1 2024     # Producto 1, solo 2024")
        print("  python visualizar_precios.py 380        # Margarina Adorita (nuevo 2025)")
        sys.exit(1)

    product_id = int(sys.argv[1])
    year_filter = int(sys.argv[2]) if len(sys.argv) > 2 else None

    # Rutas de archivos
    if year_filter == 2024:
        archivo_precios = 'data/processed/prices_aggregated_2024.csv'
    elif year_filter == 2025:
        archivo_precios = 'data/processed/prices_aggregated_2025.csv'
    else:
        archivo_precios = 'data/processed/prices_aggregated_2025.csv'

    archivo_productos = 'data/processed/products_catalog.csv'
    output_dir = 'data/visualizations'

    # Cargar datos
    precios, productos = cargar_datos(archivo_precios, archivo_productos)

    # Buscar producto
    producto = productos[productos['product_id'] == product_id]

    if producto.empty:
        print(f"❌ Producto ID {product_id} no encontrado")
        print(f"   IDs disponibles: 1-{productos['product_id'].max()}")
        sys.exit(1)

    nombre_producto = producto.iloc[0]['name']

    # Filtrar precios del producto
    precios_producto = precios[precios['product_id'] == product_id].copy()

    if year_filter:
        precios_producto = precios_producto[precios_producto['date'].dt.year == year_filter]
        titulo_periodo = f"{year_filter}"
    else:
        titulo_periodo = f"{precios_producto['date'].min().year}-{precios_producto['date'].max().year}"

    if precios_producto.empty:
        print(f"❌ No hay datos de precio para producto {product_id} en {titulo_periodo}")
        sys.exit(1)

    print(f"\n{'=' * 70}")
    print(f"Producto: {nombre_producto}")
    print(f"Período: {titulo_periodo}")
    print(f"{'=' * 70}")

    # Calcular estadísticas
    stats = calcular_estadisticas(precios_producto)

    print(f"\n📊 Estadísticas del período:")
    print(f"  • Precio mínimo histórico: ${stats['precio_min_historico']:.2f}")
    print(f"  • Precio máximo histórico: ${stats['precio_max_historico']:.2f}")
    print(f"  • Precio mediano promedio: ${stats['precio_median_promedio']:.2f}")
    print(f"  • Variación total: {stats['variacion_porcentual']:+.2f}%")
    print(f"  • Volatilidad promedio: ±${stats['volatilidad_promedio']:.2f}")
    print(f"  • Días con datos: {stats['dias_con_datos']}")
    print(f"  • Establecimientos promedio: {stats['establecimientos_promedio']:.0f}")

    # Generar gráfica
    print(f"\n📈 Generando visualización...")
    output_path = graficar_producto(precios_producto,
                                    f"{nombre_producto} ({titulo_periodo})",
                                    output_dir)

    print(f"\n✓ Gráfica guardada: {output_path}")
    print(f"\n{'=' * 70}")


if __name__ == "__main__":
    main()
