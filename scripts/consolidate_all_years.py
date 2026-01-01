#!/usr/bin/env python3
"""
Consolidate all yearly aggregated price files into a single master file.
Combines data/processed/prices_aggregated_YYYY.csv (2016-2025) into one file.
"""

import pandas as pd
from pathlib import Path
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def consolidate_all_years():
    """Consolidate all yearly aggregated files into one master file."""

    base_dir = Path(__file__).parent.parent
    processed_dir = base_dir / "data" / "processed"
    output_file = processed_dir / "prices_aggregated_all_years.csv"

    # Years to process (2016-2025)
    years = range(2016, 2026)

    all_dataframes = []
    total_records = 0

    logger.info("Starting consolidation of all yearly files...")
    logger.info(f"Processing years: {min(years)}-{max(years)}")

    # Read each year's file
    for year in years:
        file_path = processed_dir / f"prices_aggregated_{year}.csv"

        if not file_path.exists():
            logger.warning(f"File not found: {file_path}")
            continue

        try:
            df = pd.read_csv(file_path)
            records = len(df)
            total_records += records

            logger.info(f"✓ {year}: {records:,} records loaded")
            all_dataframes.append(df)

        except Exception as e:
            logger.error(f"✗ Error reading {year}: {e}")
            continue

    if not all_dataframes:
        logger.error("No data files were loaded. Aborting.")
        return

    # Concatenate all dataframes
    logger.info("\nCombining all years into single dataset...")
    combined_df = pd.concat(all_dataframes, ignore_index=True)

    # Convert date to proper datetime format (handle both date and datetime formats)
    logger.info("Normalizing date format...")
    combined_df['date'] = pd.to_datetime(combined_df['date'], format='mixed').dt.date

    # Sort by product_id and date for better organization
    logger.info("Sorting by product_id and date...")
    combined_df = combined_df.sort_values(['product_id', 'date']).reset_index(drop=True)

    # Get statistics
    unique_products = combined_df['product_id'].nunique()
    min_date = combined_df['date'].min()
    max_date = combined_df['date'].max()

    # Write to file
    logger.info(f"\nWriting consolidated file to: {output_file}")
    combined_df.to_csv(output_file, index=False, date_format='%Y-%m-%d')

    file_size_mb = output_file.stat().st_size / (1024 * 1024)

    # Print summary
    logger.info("\n" + "="*60)
    logger.info("CONSOLIDATION COMPLETE")
    logger.info("="*60)
    logger.info(f"Output file: {output_file.name}")
    logger.info(f"File size: {file_size_mb:.2f} MB")
    logger.info(f"Total records: {len(combined_df):,}")
    logger.info(f"Unique products: {unique_products}")
    logger.info(f"Date range: {min_date} to {max_date}")
    logger.info(f"Years covered: {max(years) - min(years) + 1}")
    logger.info("="*60)

    # Verify record count matches
    if len(combined_df) == total_records:
        logger.info("✓ Record count verification PASSED")
    else:
        logger.warning(f"⚠ Record count mismatch: expected {total_records:,}, got {len(combined_df):,}")


if __name__ == "__main__":
    consolidate_all_years()
