# AGENTS.md

This file provides guidance to AI Agent when working with code in this repository.

## Project Overview

**CanastaUY** is a **learning project** focused on Uruguayan product pricing data analysis and backend development (2016-2025). The project will eventually become a monorepo containing data science pipelines, a Spring Boot backend API, and a frontend (future).

**Important**: This is a learning project. All architectural and technology decisions should be made by the project owner. Claude should focus on boilerplate code, repetitive tasks, and execution of decisions already made.

## Documentation Style Guidelines

- **Do not use emojis** in documentation files (Markdown files in `docs/`)
- Use plain text for status indicators (e.g., "Complete", "In Progress", "Pending" instead of emojis)

**Current Dataset**:

- 817,842 price records across 379 unique products
- 10 years of historical data (2016-2025)
- Consolidated file: `data/processed/prices_aggregated_all_years.csv` (42 MB)
- Product catalog: `data/processed/products_catalog.csv` (379 products)
- Product groups by category: `data/processed/product_groups_by_category.json` (379 products in 193 categories)

**Data Schema**:

- **Prices table**: `product_id, date, price_min, price_max, price_avg, price_median, price_std, store_count, offer_count, offer_percentage`
- **Products table**: `product_id, category, brand, specification, name`

## Setup and Dependencies

**Python Version**: 3.12+

**Dependency Manager**: `uv` (must be installed)

Install dependencies:

```bash
uv sync
```

Run scripts:

```bash
uv run python scripts/script_name.py
```

**IDE Configuration**:

- Neovim/LazyVim automatically detects `pyrightconfig.json` for Python type checking
- Make sure to run `uv sync` after cloning so the venv is created

**Current Dependencies**:

- `pandas>=2.3.3` - Data manipulation
- `matplotlib>=3.10.8` - Visualization
- `seaborn>=0.13.2` - Statistical visualization

## Project Structure

```
open-price-uy/
├── data/
│   └── processed/                # Processed CSV files ready for analysis/backend
│       └── prices_aggregated_all_years_v4.csv  ← USE THIS FOR ANALYSIS
│
├── backend/                      # Future Spring Boot API
│   ├── src/main/java/.../
│   └── src/main/resources/
│       └── db/migration/         # Flyway/Liquibase migrations
│           └── V1__initial_schema.sql
│
├── scripts/                      # Python scripts for data pipeline
│   ├── consolidate_all_years.py             # Merge yearly CSVs
│   ├── consolidate_products.py              # Merge product catalogs
│   ├── detect_price_outliers.py             # Create V2 (3x method)
│   ├── detect_outliers_v4_improved.py       # Create V4 (IQR - RECOMMENDED)
│   ├── create_clean_dataset_v3.py           # Create V3 (for reference)
│   ├── group_products_by_category.py        # Generate category mapping JSON
│   ├── product_descriptive_statistics_v4.py # Generate statistics for V4
│   ├── visualize_rice_price_evolution_v4.py # Final rice evolution chart
│   ├── visualize_rice_price_distribution.py # Rice distribution by brand
│   ├── visualize_rice_min_max_distribution.py # Explains IQR detection
│   │
│   └── deprecated/               # Old/archived scripts
│       ├── agregar_precios_por_producto.py
│       ├── filtrar_precios.py
│       ├── analyze_outliers.py
│       ├── analyze_products.py
│       ├── investigate_rice_extremes.py
│       ├── investigate_rice_extremes_v3.py
│       └── investigate_price_min_max.py
│
├── outputs/                      # Generated visualizations
│   ├── rice_price_evolution_v4.png
│   ├── rice_price_distribution_comparison.png
│   ├── rice_min_max_comparison.png
│   └── outlier_analysis.png
│
├── reports/                      # Future analysis reports & dashboards
│
├── docs/                         # Comprehensive documentation
│   ├── dataset-versions.md       # Detailed v1-v4 comparison
│   ├── outlier-detection-methodology.md  # Detection methods explained
│   ├── data-cleaning-pipeline.md # Complete walkthrough & usage guide
│   └── database-schema.md        # PostgreSQL schema for REST API
│
├── main.py                       # Entry point (placeholder)
├── pyproject.toml               # Project metadata & dependencies
├── uv.lock                      # Locked dependency versions
├── PROJECT_CLEANUP_ANALYSIS.md  # Rigorous cleanup decision document
└── CLAUDE.md                    # This file
```

## Key Scripts (Production Pipeline)

**Data Consolidation:**

- **`consolidate_all_years.py`**: Merges all yearly aggregated files (2016-2025) → master file
- **`consolidate_products.py`**: Consolidates product catalog data

**Data Cleaning (Version Pipeline):**

- **`detect_price_outliers.py`**: Creates V2 (adds category, year_month, outlier flags using 3x method)
- **`detect_outliers_v4_improved.py`**: Creates V4 (improved IQR detection) - **RECOMMENDED**
- **`create_clean_dataset_v3.py`**: Creates V3 (reference dataset using 3x method)

**Utilities:**

- **`group_products_by_category.py`**: Groups 379 products into 193 categories → JSON mapping

**Analysis & Visualization:**

- **`product_descriptive_statistics_v4.py`**: Generates comprehensive statistics for V4 data
- **`visualize_rice_price_evolution_v4.py`**: Final rice price evolution chart (clean V4 data)
- **`visualize_rice_price_distribution.py`**: Rice distribution analysis by brand
- **`visualize_rice_min_max_distribution.py`**: Explains why IQR catches $359/$228 anomalies

## Common Commands

```bash
# Install dependencies
uv sync

# Run pipeline to regenerate all data versions
uv run python scripts/consolidate_all_years.py
uv run python scripts/detect_price_outliers.py       # V2
uv run python scripts/create_clean_dataset_v3.py     # V3
uv run python scripts/detect_outliers_v4_improved.py # V4

# Generate analysis & visualizations (uses V4)
uv run python scripts/product_descriptive_statistics_v4.py
uv run python scripts/visualize_rice_price_evolution_v4.py
uv run python scripts/visualize_rice_price_distribution.py

# Run main entry point
uv run python main.py
```

## Data Processing Pipeline

1. **Raw Data** → **Processed CSVs** (yearly files, 2016-2025)
2. **Yearly CSVs** → **Consolidated Master File** (`prices_aggregated_all_years.csv`)
3. **Product Catalog** → **Product Groups** (`product_groups_by_category.json`)
4. **Consolidated Data + Product Groups** → **Visualizations & Insights**
5. **Consolidated Data** → **Database Import** (PostgreSQL) → **Backend API** (Spring Boot)

## Database Schema (PostgreSQL)

The API uses a hybrid schema with raw daily data and pre-aggregated monthly cache for optimal query performance.

**Tables:**

- `categories` - 2-level hierarchy (category → subcategory)
- `products` - product catalog with category reference
- `prices_daily` - raw daily snapshots (daily granularity)
- `prices_monthly_cache` - pre-aggregated monthly data (optimized analytics)

**Migration file:** `backend/src/main/resources/db/migration/V1__initial_schema.sql`

See `docs/database-schema.md` for full schema documentation.

## Dataset Versions

The project now has multiple cleaned versions of the price data:

- **v1 (Original)**: `prices_aggregated_all_years.csv` - Raw, unclean data with extremes ($4,557 max)
- **v2 (Labeled)**: `prices_aggregated_all_years_v2.csv` - Added columns: `category`, `year_month`, `outlier` (0/1 using 3x method)
- **v3 (3x Clean)**: `prices_aggregated_all_years_v3.csv` - V2 with outlier=1 records removed (0.74% removed)
- **v4 (IQR Clean)**: `prices_aggregated_all_years_v4.csv` - **RECOMMENDED** for analysis (5.27% removed using IQR method)

**Use v4 by default** - it uses the IQR method which is more robust for outlier detection.

See `docs/dataset-versions.md` for detailed comparison.

## Product Grouping Reference

The `product_groups_by_category.json` file maps 379 products into 193 exact categories. This enables:

- **Category-based analysis**: Compare price trends across similar products
- **Anomaly detection**: Identify outliers within product categories
- **Aggregated analytics**: Calculate category-level statistics
- **Bulk visualizations**: Create multi-product comparison charts

**Example usage in scripts**:

```python
import json
with open("data/processed/product_groups_by_category.json") as f:
    groups = json.load(f)

# Get all rice products
rice_ids = groups['categories']['Arroz blanco']  # [15, 16, 17, 18, 19, 20]

# Filter price data by category
rice_prices = prices_df[prices_df['product_id'].isin(rice_ids)]
```

## Important Notes for Next Session

1. **Data Quality Issues Found**: Fresh produce (bananas, tomatoes, etc.) show extreme price variations (CV > 100%) - likely due to seasonal availability and unit inconsistencies (kg vs individual unit)
2. **Outlier Detection Strategy**: Moved from simple 3x threshold (v3) to IQR method (v4) for more robust detection
3. **Key Anomaly**: Single instance of $359 max price for Green Chef rice in 2016-07-20 (likely data error)
4. **Products to Watch**: Jabón de Tocador, Gaseosa Nix, Banana Brasil - high outlier percentages even in v4
5. **Data Retention**: V4 retains 94.73% of records while removing only the most problematic outliers

## Project Organization Notes

### Scripts Hierarchy

1. **Production scripts** (actively maintained): 10 scripts in `scripts/`
2. **Deprecated scripts** (archived): 7 scripts in `scripts/deprecated/`
   - Kept for archaeological/debugging purposes
   - Not part of the active pipeline
   - Feel free to delete if space is needed

### Data Files Management

- **Keep**: V4 and earlier versions (v1-v3) for reproducibility
- **Use for analysis**: V4 exclusively (IQR method, statistically rigorous)
- **Archive**: Raw yearly files can be deleted after consolidation is proven stable

### Cleanup Decision Framework

Scripts were evaluated on:

- **Value**: Essential to pipeline or one-time investigation?
- **Maturity**: Production-ready or experimental?
- **Reusability**: Used regularly or was it debugging?
- **Dependency**: Does output still exist?
- **Redundancy**: Is better version available?

See `PROJECT_CLEANUP_ANALYSIS.md` for detailed rationale.

## Notes for Future Development

- **Backend Integration**: Use `prices_aggregated_all_years_v4.csv` for database import (recommended: PostgreSQL)
- **Data Size**: 774k records (V4) presents performance challenges—implement indexing, pagination, caching
- **Monorepo Structure**: Plan for `/backend` (Spring Boot), `/frontend` (future), `/data-science` (current scripts)
- **Blog Documentation**: Comprehensive pipeline documentation already exists in `docs/`
