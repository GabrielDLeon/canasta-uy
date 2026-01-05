"""
Investigates what price_min and price_max columns actually represent.
These are different from price_avg - let's understand what they are.
"""

import pandas as pd
from pathlib import Path

print("="*80)
print("INVESTIGATING price_min AND price_max COLUMNS")
print("="*80)

# Load v3
v3_path = Path(__file__).parent.parent / "data" / "processed" / "prices_aggregated_all_years_v3.csv"
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"

prices_df = pd.read_csv(v3_path)
products_df = pd.read_csv(products_path)

prices_df['date'] = pd.to_datetime(prices_df['date'])

# Get rice data
rice_product_ids = products_df[products_df['category'] == 'Arroz blanco']['product_id'].tolist()
rice_prices = prices_df[prices_df['product_id'].isin(rice_product_ids)].copy()
rice_prices['year'] = rice_prices['date'].dt.year

print("\n" + "="*80)
print("UNDERSTANDING THE COLUMNS")
print("="*80)

print("\nFirst record from v3:")
first_record = prices_df.iloc[0]
print(f"  product_id: {first_record['product_id']}")
print(f"  date: {first_record['date']}")
print(f"  price_min: ${first_record['price_min']:.2f}")
print(f"  price_max: ${first_record['price_max']:.2f}")
print(f"  price_avg: ${first_record['price_avg']:.2f}")
print(f"  price_median: ${first_record['price_median']:.2f}")
print(f"  store_count: {first_record['store_count']}")
print(f"  offer_count: {first_record['offer_count']}")

print("\nThese columns likely represent:")
print("  price_min: minimum price observed in stores on that date")
print("  price_max: maximum price observed in stores on that date")
print("  price_avg: average price across all stores")
print("  price_median: median price across all stores")

print("\n" + "="*80)
print("RICE PRICE_MIN AND PRICE_MAX IN 2016")
print("="*80)

rice_2016 = rice_prices[rice_prices['year'] == 2016]

print(f"\nRice data in 2016:")
print(f"  Total records: {len(rice_2016)}")
print(f"  price_min range: ${rice_2016['price_min'].min():.2f} - ${rice_2016['price_min'].max():.2f}")
print(f"  price_max range: ${rice_2016['price_max'].min():.2f} - ${rice_2016['price_max'].max():.2f}")
print(f"  price_avg range: ${rice_2016['price_avg'].min():.2f} - ${rice_2016['price_avg'].max():.2f}")

print(f"\nExtreme price_min values in 2016:")
extreme_min = rice_2016.nsmallest(5, 'price_min')[['product_id', 'date', 'price_min', 'price_max', 'price_avg']]
print(extreme_min)

print(f"\nExtreme price_max values in 2016:")
extreme_max = rice_2016.nlargest(5, 'price_max')[['product_id', 'date', 'price_min', 'price_max', 'price_avg']]
print(extreme_max)

print("\n" + "="*80)
print("RICE PRICE_MIN AND PRICE_MAX IN 2025")
print("="*80)

rice_2025 = rice_prices[rice_prices['year'] == 2025]

print(f"\nRice data in 2025:")
print(f"  Total records: {len(rice_2025)}")
print(f"  price_min range: ${rice_2025['price_min'].min():.2f} - ${rice_2025['price_min'].max():.2f}")
print(f"  price_max range: ${rice_2025['price_max'].min():.2f} - ${rice_2025['price_max'].max():.2f}")
print(f"  price_avg range: ${rice_2025['price_avg'].min():.2f} - ${rice_2025['price_avg'].max():.2f}")

print(f"\nExtreme price_min values in 2025:")
extreme_min_2025 = rice_2025.nsmallest(5, 'price_min')[['product_id', 'date', 'price_min', 'price_max', 'price_avg']]
print(extreme_min_2025)

print(f"\nExtreme price_max values in 2025:")
extreme_max_2025 = rice_2025.nlargest(5, 'price_max')[['product_id', 'date', 'price_min', 'price_max', 'price_avg']]
print(extreme_max_2025)

# Find the record with $359 or $228
print("\n" + "="*80)
print("SEARCHING FOR THE EXTREME VALUES ($359, $228)")
print("="*80)

records_359 = rice_prices[
    (rice_prices['price_min'] >= 350) |
    (rice_prices['price_max'] >= 350)
]

print(f"\nRecords with price_min or price_max >= $350:")
if len(records_359) > 0:
    print(records_359[['product_id', 'date', 'price_min', 'price_max', 'price_avg', 'year']])
else:
    print("  None found")

records_228 = rice_prices[
    (rice_prices['price_min'] >= 220) |
    (rice_prices['price_max'] >= 220)
]

print(f"\nRecords with price_min or price_max >= $220:")
if len(records_228) > 0:
    print(records_228[['product_id', 'date', 'price_min', 'price_max', 'price_avg', 'year']])
else:
    print("  None found")

# Show sample of a single date to understand the data better
print("\n" + "="*80)
print("SAMPLE: All rice products on one specific date (2016-01-01)")
print("="*80)

sample_date = rice_prices[rice_prices['date'] == '2016-01-01']
if len(sample_date) > 0:
    print(f"\nFound {len(sample_date)} records on 2016-01-01:")
    for idx, row in sample_date.iterrows():
        prod = products_df[products_df['product_id'] == row['product_id']].iloc[0]
        print(f"\n  {prod['name']}:")
        print(f"    price_min: ${row['price_min']:.2f}")
        print(f"    price_max: ${row['price_max']:.2f}")
        print(f"    price_avg: ${row['price_avg']:.2f}")
        print(f"    stores: {row['store_count']}")
else:
    print("\nNo records found for 2016-01-01, trying another date...")
    sample_date = rice_prices[rice_prices['year'] == 2016].iloc[0:6]
    for idx, row in sample_date.iterrows():
        prod = products_df[products_df['product_id'] == row['product_id']].iloc[0]
        print(f"\n  {row['date'].date()} - {prod['name']}:")
        print(f"    price_min: ${row['price_min']:.2f}")
        print(f"    price_max: ${row['price_max']:.2f}")
        print(f"    price_avg: ${row['price_avg']:.2f}")
        print(f"    stores: {row['store_count']}")

print("\n" + "="*80)
