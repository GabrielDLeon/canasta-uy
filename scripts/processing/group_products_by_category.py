"""
Groups products by exact category and generates a JSON file with product IDs.
This file can be reused across multiple scripts for analysis, anomaly detection, and grouping.
"""

import pandas as pd
import json
from pathlib import Path
from collections import defaultdict

# Load products catalog
products_path = Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"
products_df = pd.read_csv(products_path)

print(f"Total products loaded: {len(products_df)}")
print(f"Total unique categories: {products_df['category'].nunique()}")

# Group products by category
category_groups = defaultdict(list)

for _, row in products_df.iterrows():
    product_id = int(row['product_id'])
    category = row['category']
    category_groups[category].append(product_id)

# Convert to regular dict and sort product IDs
category_groups = {
    category: sorted(product_ids)
    for category, product_ids in sorted(category_groups.items())
}

# Generate summary statistics
print("\n" + "="*80)
print("CATEGORY SUMMARY")
print("="*80)

summary_stats = []
for category, product_ids in category_groups.items():
    count = len(product_ids)
    summary_stats.append({
        'category': category,
        'count': count,
        'product_ids': product_ids
    })
    print(f"{category:50s} → {count:3d} productos")

print("="*80)

# Save to JSON file
output_path = Path(__file__).parent.parent / "data" / "processed" / "product_groups_by_category.json"

output_data = {
    "metadata": {
        "total_products": len(products_df),
        "total_categories": len(category_groups),
        "description": "Product IDs grouped by exact category name",
        "generated_by": "group_products_by_category.py"
    },
    "categories": category_groups
}

with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)

print(f"\n✓ JSON file saved to: {output_path}")
print(f"  - Total categories: {len(category_groups)}")
print(f"  - Total products: {len(products_df)}")

# Show examples of usage
print("\n" + "="*80)
print("USAGE EXAMPLES")
print("="*80)
print("""
# To use this file in your scripts:

import json
from pathlib import Path

# Load the product groups
groups_path = Path("data/processed/product_groups_by_category.json")
with open(groups_path, 'r', encoding='utf-8') as f:
    product_groups = json.load(f)

# Get all rice product IDs
rice_ids = product_groups['categories']['Arroz blanco']
print(f"Rice products: {rice_ids}")

# Get all oil products (need to combine multiple categories)
oil_categories = [cat for cat in product_groups['categories'].keys()
                  if 'Aceite' in cat]
oil_ids = []
for cat in oil_categories:
    oil_ids.extend(product_groups['categories'][cat])

# Iterate over all categories
for category, product_ids in product_groups['categories'].items():
    print(f"{category}: {len(product_ids)} products")
""")
print("="*80)
