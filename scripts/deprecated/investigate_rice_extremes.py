"""
Investigates extreme values in rice price data.
Examines min/max prices to understand why they weren't flagged as outliers.
"""

import pandas as pd
import json
from pathlib import Path

print("="*80)
print("RICE PRICE EXTREMES INVESTIGATION")
print("="*80)

# Load data
v2_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v2.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v2_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Filter rice products
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()

print(f"\nRice data summary:")
print(f"  Total records: {len(rice_prices):,}")
print(f"  Price range: ${rice_prices['price_avg'].min():.2f} - ${rice_prices['price_avg'].max():.2f}")
print(f"  Outliers: {rice_prices['outlier'].sum()}")

# Find the extreme values
print("\n" + "="*80)
print("EXTREME HIGH PRICES")
print("="*80)

high_prices = rice_prices.nlargest(10, 'price_avg')[['product_id', 'date', 'price_avg', 'year_month', 'outlier']]
for idx, row in high_prices.iterrows():
    prod = products_df[products_df['product_id'] == row['product_id']].iloc[0]
    status = "OUTLIER" if row['outlier'] == 1 else "NORMAL"
    print(f"\nProduct ID {row['product_id']}: {prod['name']}")
    print(f"  Date: {row['date'].date()}")
    print(f"  Price: ${row['price_avg']:.2f}")
    print(f"  Window: {row['year_month']}")
    print(f"  Status: {status}")

print("\n" + "="*80)
print("EXTREME LOW PRICES")
print("="*80)

low_prices = rice_prices.nsmallest(10, 'price_avg')[['product_id', 'date', 'price_avg', 'year_month', 'outlier']]
for idx, row in low_prices.iterrows():
    prod = products_df[products_df['product_id'] == row['product_id']].iloc[0]
    status = "OUTLIER" if row['outlier'] == 1 else "NORMAL"
    print(f"\nProduct ID {row['product_id']}: {prod['name']}")
    print(f"  Date: {row['date'].date()}")
    print(f"  Price: ${row['price_avg']:.2f}")
    print(f"  Window: {row['year_month']}")
    print(f"  Status: {status}")

# Analyze the window where the extreme high price occurs
print("\n" + "="*80)
print("DETAILED ANALYSIS: Window containing max price ($359.0)")
print("="*80)

max_price_record = rice_prices[rice_prices['price_avg'] == rice_prices['price_avg'].max()].iloc[0]
max_window = max_price_record['year_month']
max_product = max_price_record['product_id']

print(f"Max price occurred in: {max_window}")
print(f"Product ID: {max_product}")
print(f"Date: {max_price_record['date'].date()}")

# Get all records in that window
window_records = rice_prices[rice_prices['year_month'] == max_window]

print(f"\nAll rice records in this window ({max_window}):")
print(f"  Total records: {len(window_records)}")

# Group by product to see prices by product in this window
for product_id in sorted(rice_product_ids):
    prod_in_window = window_records[window_records['product_id'] == product_id]
    if len(prod_in_window) > 0:
        prod = products_df[products_df['product_id'] == product_id].iloc[0]
        prices = prod_in_window['price_avg'].values
        print(f"\n  {prod['name']}:")
        print(f"    Records: {len(prices)}")
        print(f"    Min: ${prices.min():.2f}")
        print(f"    Max: ${prices.max():.2f}")
        print(f"    Mean: ${prices.mean():.2f}")
        print(f"    Median: ${pd.Series(prices).median():.2f}")
        print(f"    Outliers in window: {prod_in_window['outlier'].sum()}")

# Calculate group median and thresholds for this window
print(f"\nGroup Statistics for {max_window} (all rice products):")
window_median = window_records['price_avg'].median()
window_mean = window_records['price_avg'].mean()
lower_threshold = window_median / 3
upper_threshold = window_median * 3

print(f"  Median price: ${window_median:.2f}")
print(f"  Mean price: ${window_mean:.2f}")
print(f"  Lower threshold (median/3): ${lower_threshold:.2f}")
print(f"  Upper threshold (median*3): ${upper_threshold:.2f}")

# Check the max price against thresholds
max_price = rice_prices['price_avg'].max()
ratio = max_price / window_median

print(f"\n  Max price: ${max_price:.2f}")
print(f"  Ratio vs median: {ratio:.2f}x")
print(f"  Should be flagged as outlier: {max_price > upper_threshold}")

# Let's also check the min price
print("\n" + "="*80)
print("DETAILED ANALYSIS: Window containing min price ($6.6)")
print("="*80)

min_price_record = rice_prices[rice_prices['price_avg'] == rice_prices['price_avg'].min()].iloc[0]
min_window = min_price_record['year_month']

print(f"Min price occurred in: {min_window}")
print(f"Product ID: {min_price_record['product_id']}")
print(f"Date: {min_price_record['date'].date()}")

# Get all records in that window
window_records_min = rice_prices[rice_prices['year_month'] == min_window]

print(f"\nAll rice records in this window ({min_window}):")
print(f"  Total records: {len(window_records_min)}")

# Calculate group median and thresholds for this window
window_median_min = window_records_min['price_avg'].median()
lower_threshold_min = window_median_min / 3
upper_threshold_min = window_median_min * 3

print(f"  Median price: ${window_median_min:.2f}")
print(f"  Lower threshold (median/3): ${lower_threshold_min:.2f}")
print(f"  Upper threshold (median*3): ${upper_threshold_min:.2f}")

min_price = rice_prices['price_avg'].min()
ratio_min = min_price / window_median_min

print(f"\n  Min price: ${min_price:.2f}")
print(f"  Ratio vs median: {ratio_min:.2f}x")
print(f"  Should be flagged as outlier: {min_price < lower_threshold_min}")

print("\n" + "="*80)
