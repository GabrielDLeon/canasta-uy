#!/usr/bin/env python3
"""
Generate insightful visualizations from consolidated price data (2016-2025).
Creates multiple charts analyzing inflation, volatility, offers, and trends.
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import numpy as np
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Set style
plt.style.use('seaborn-v0_8-darkgrid')
sns.set_palette("husl")


def load_data():
    """Load consolidated price data and product catalog."""
    base_dir = Path(__file__).parent.parent

    logger.info("Loading consolidated data...")
    prices = pd.read_csv(base_dir / "data" / "processed" / "prices_aggregated_all_years.csv")
    prices['date'] = pd.to_datetime(prices['date'])
    prices['year'] = prices['date'].dt.year
    prices['month'] = prices['date'].dt.month

    logger.info("Loading product catalog...")
    products = pd.read_csv(base_dir / "data" / "processed" / "products_catalog.csv")

    # Merge to get category information
    df = prices.merge(products[['product_id', 'category', 'name']], on='product_id', how='left')

    logger.info(f"Data loaded: {len(df):,} records, {df['product_id'].nunique()} products")
    return df, products


def inflation_heatmap(df, output_dir):
    """1. Heatmap de Inflación por Categoría (2016-2025)."""
    logger.info("Creating inflation heatmap by category...")

    # Calculate yearly average price per category
    yearly_cat = df.groupby(['year', 'category'])['price_avg'].mean().reset_index()

    # Calculate year-over-year inflation rate
    inflation_data = []
    for category in yearly_cat['category'].unique():
        cat_data = yearly_cat[yearly_cat['category'] == category].sort_values('year')
        if len(cat_data) < 2:
            continue

        for i in range(1, len(cat_data)):
            prev_price = cat_data.iloc[i-1]['price_avg']
            curr_price = cat_data.iloc[i]['price_avg']
            inflation_rate = ((curr_price - prev_price) / prev_price) * 100

            inflation_data.append({
                'year': cat_data.iloc[i]['year'],
                'category': category,
                'inflation_rate': inflation_rate
            })

    inflation_df = pd.DataFrame(inflation_data)

    # Pivot for heatmap
    pivot = inflation_df.pivot(index='category', columns='year', values='inflation_rate')

    # Create heatmap
    fig, ax = plt.subplots(figsize=(14, 10))
    sns.heatmap(pivot, annot=True, fmt='.1f', cmap='RdYlGn_r', center=0,
                cbar_kws={'label': 'Inflación Anual (%)'}, linewidths=0.5)
    plt.title('Mapa de Calor: Inflación Anual por Categoría (2016-2025)',
              fontsize=16, fontweight='bold', pad=20)
    plt.xlabel('Año', fontsize=12)
    plt.ylabel('Categoría', fontsize=12)
    plt.xticks(rotation=0)
    plt.yticks(rotation=0, fontsize=8)
    plt.tight_layout()

    output_file = output_dir / "01_inflation_heatmap.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def covid_impact(df, output_dir):
    """2. Análisis de Impacto COVID-19."""
    logger.info("Creating COVID-19 impact analysis...")

    # Define periods
    df['period'] = pd.cut(df['year'],
                          bins=[2015, 2019, 2021, 2025],
                          labels=['Pre-COVID\n(2016-2019)',
                                  'Durante COVID\n(2020-2021)',
                                  'Post-COVID\n(2022-2025)'])

    # Calculate average price per period and category
    period_avg = df.groupby(['period', 'category'])['price_avg'].mean().reset_index()

    # Get top 15 categories by overall average
    top_categories = df.groupby('category')['price_avg'].mean().nlargest(15).index
    period_avg_top = period_avg[period_avg['category'].isin(top_categories)]

    # Create grouped bar chart
    fig, ax = plt.subplots(figsize=(14, 8))

    categories = sorted(period_avg_top['category'].unique())
    x = np.arange(len(categories))
    width = 0.25

    periods = period_avg_top['period'].unique()
    for i, period in enumerate(periods):
        data = period_avg_top[period_avg_top['period'] == period]
        data = data.set_index('category').reindex(categories)
        ax.bar(x + i*width, data['price_avg'], width, label=period)

    ax.set_xlabel('Categoría', fontsize=12)
    ax.set_ylabel('Precio Promedio ($)', fontsize=12)
    ax.set_title('Impacto COVID-19: Precios por Período', fontsize=16, fontweight='bold')
    ax.set_xticks(x + width)
    ax.set_xticklabels(categories, rotation=45, ha='right', fontsize=8)
    ax.legend()
    ax.grid(True, alpha=0.3)
    plt.tight_layout()

    output_file = output_dir / "02_covid_impact.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def top_price_changes(df, output_dir):
    """3. Top 20 Productos que Más Subieron/Bajaron."""
    logger.info("Creating top price changes chart...")

    # Calculate price change from 2016 to 2025 for each product
    price_changes = []
    for product_id in df['product_id'].unique():
        product_data = df[df['product_id'] == product_id]

        # Get 2016 and 2025 average prices
        price_2016 = product_data[product_data['year'] == 2016]['price_avg'].mean()
        price_2025 = product_data[product_data['year'] == 2025]['price_avg'].mean()

        if pd.notna(price_2016) and pd.notna(price_2025) and price_2016 > 0:
            change_pct = ((price_2025 - price_2016) / price_2016) * 100
            product_name = product_data['name'].iloc[0] if 'name' in product_data.columns else f"Producto {product_id}"

            price_changes.append({
                'product_id': product_id,
                'name': product_name,
                'change_pct': change_pct,
                'price_2016': price_2016,
                'price_2025': price_2025
            })

    changes_df = pd.DataFrame(price_changes)

    # Get top 10 gainers and losers
    top_gainers = changes_df.nlargest(10, 'change_pct')
    top_losers = changes_df.nsmallest(10, 'change_pct')

    # Create subplots
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 8))

    # Top gainers
    colors1 = ['red' if x > 0 else 'green' for x in top_gainers['change_pct']]
    ax1.barh(range(len(top_gainers)), top_gainers['change_pct'], color=colors1, alpha=0.7)
    ax1.set_yticks(range(len(top_gainers)))
    ax1.set_yticklabels([name[:40] + '...' if len(name) > 40 else name
                         for name in top_gainers['name']], fontsize=9)
    ax1.set_xlabel('Cambio de Precio (%)', fontsize=12)
    ax1.set_title('Top 10: Mayor Incremento (2016→2025)', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3, axis='x')
    ax1.invert_yaxis()

    # Top losers
    colors2 = ['red' if x > 0 else 'green' for x in top_losers['change_pct']]
    ax2.barh(range(len(top_losers)), top_losers['change_pct'], color=colors2, alpha=0.7)
    ax2.set_yticks(range(len(top_losers)))
    ax2.set_yticklabels([name[:40] + '...' if len(name) > 40 else name
                         for name in top_losers['name']], fontsize=9)
    ax2.set_xlabel('Cambio de Precio (%)', fontsize=12)
    ax2.set_title('Top 10: Mayor Disminución (2016→2025)', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3, axis='x')
    ax2.invert_yaxis()

    plt.tight_layout()
    output_file = output_dir / "03_top_price_changes.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def offer_calendar(df, output_dir):
    """4. Calendario de Ofertas - Patrón Mensual."""
    logger.info("Creating offer calendar...")

    # Calculate average offer percentage by month across all years
    monthly_offers = df.groupby(['month', 'category'])['offer_percentage'].mean().reset_index()

    # Get top categories by overall offer percentage
    top_offer_cats = df.groupby('category')['offer_percentage'].mean().nlargest(15).index
    monthly_top = monthly_offers[monthly_offers['category'].isin(top_offer_cats)]

    # Pivot for heatmap
    pivot = monthly_top.pivot(index='category', columns='month', values='offer_percentage')

    # Create heatmap
    fig, ax = plt.subplots(figsize=(12, 10))
    sns.heatmap(pivot, annot=True, fmt='.1f', cmap='YlOrRd',
                cbar_kws={'label': '% de Productos en Oferta'}, linewidths=0.5)
    plt.title('Calendario de Ofertas: Patrón Mensual por Categoría (2016-2025)',
              fontsize=16, fontweight='bold', pad=20)
    plt.xlabel('Mes', fontsize=12)
    plt.ylabel('Categoría', fontsize=12)
    ax.set_xticklabels(['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                        'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'], rotation=0)
    plt.yticks(rotation=0, fontsize=9)
    plt.tight_layout()

    output_file = output_dir / "04_offer_calendar.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def volatility_analysis(df, output_dir):
    """5. Análisis de Volatilidad por Categoría."""
    logger.info("Creating volatility analysis...")

    # Calculate average volatility (std) per category
    volatility = df.groupby('category')['price_std'].mean().sort_values(ascending=False)

    # Create horizontal bar chart
    fig, ax = plt.subplots(figsize=(12, 10))
    colors = plt.cm.viridis(np.linspace(0, 1, len(volatility)))
    ax.barh(range(len(volatility)), volatility.values, color=colors, alpha=0.8)
    ax.set_yticks(range(len(volatility)))
    ax.set_yticklabels(volatility.index, fontsize=9)
    ax.set_xlabel('Desviación Estándar Promedio ($)', fontsize=12)
    ax.set_title('Volatilidad de Precios por Categoría (2016-2025)',
                 fontsize=16, fontweight='bold')
    ax.grid(True, alpha=0.3, axis='x')
    ax.invert_yaxis()
    plt.tight_layout()

    output_file = output_dir / "05_volatility_by_category.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def basket_evolution(df, products, output_dir):
    """6. Evolución de Canasta Básica (2016-2025)."""
    logger.info("Creating basic basket evolution...")

    # Define a basic basket (common products)
    basic_basket = [
        'Arroz blanco',
        'Aceite de girasol',
        'Azúcar blanco',
        'Leche entera UHT',
        'Pan flauta',
        'Carne picada vacuna',
        'Fideos secos',
        'Huevos color blancos',
        'Yerba mate'
    ]

    # Filter products in basket
    basket_df = df[df['category'].isin(basic_basket)]

    # Calculate total basket cost per year (average of all products)
    yearly_basket = basket_df.groupby('year')['price_avg'].sum().reset_index()

    # Create line chart
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.plot(yearly_basket['year'], yearly_basket['price_avg'],
            marker='o', linewidth=3, markersize=10, color='darkblue')

    # Add value labels
    for x, y in zip(yearly_basket['year'], yearly_basket['price_avg']):
        ax.text(x, y + 20, f'${y:.0f}', ha='center', fontsize=10, fontweight='bold')

    ax.set_xlabel('Año', fontsize=12)
    ax.set_ylabel('Costo Total ($)', fontsize=12)
    ax.set_title('Evolución del Costo de Canasta Básica (2016-2025)',
                 fontsize=16, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.set_xticks(yearly_basket['year'])

    # Calculate total increase
    total_increase = ((yearly_basket['price_avg'].iloc[-1] - yearly_basket['price_avg'].iloc[0])
                     / yearly_basket['price_avg'].iloc[0] * 100)
    ax.text(0.5, 0.95, f'Incremento total: {total_increase:.1f}%',
            transform=ax.transAxes, ha='center', fontsize=12,
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

    plt.tight_layout()
    output_file = output_dir / "06_basket_evolution.png"
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()
    logger.info(f"✓ Saved: {output_file.name}")


def main():
    """Generate all visualizations."""

    # Create output directory
    base_dir = Path(__file__).parent.parent
    output_dir = base_dir / "visualizations"
    output_dir.mkdir(exist_ok=True)

    logger.info("="*60)
    logger.info("OPEN PRICE UY - INSIGHTS VISUALIZATION")
    logger.info("="*60)

    # Load data
    df, products = load_data()

    # Generate visualizations
    try:
        inflation_heatmap(df, output_dir)
        covid_impact(df, output_dir)
        top_price_changes(df, output_dir)
        offer_calendar(df, output_dir)
        volatility_analysis(df, output_dir)
        basket_evolution(df, products, output_dir)

        logger.info("\n" + "="*60)
        logger.info("ALL VISUALIZATIONS COMPLETED")
        logger.info(f"Output directory: {output_dir}")
        logger.info("="*60)

    except Exception as e:
        logger.error(f"Error generating visualizations: {e}")
        raise


if __name__ == "__main__":
    main()
