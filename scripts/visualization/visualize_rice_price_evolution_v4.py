"""
Rice price evolution visualization (2016-2025) - V4 IMPROVED CLEAN DATA.

Uses prices_aggregated_all_years_v4.csv (outliers removed with IQR method).
Shows average, median, and min-max range for all rice products.
"""

import os
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Set style
sns.set_style("whitegrid")
plt.rcParams["figure.figsize"] = (14, 8)

# Load data
data_path = (
    Path(__file__).parent.parent
    / "data"
    / "processed"
    / "prices_aggregated_all_years_v4.csv"
)
products_path = (
    Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"
)

print("Loading clean v4 data...")
prices_df = pd.read_csv(data_path)
products_df = pd.read_csv(products_path)

prices_df["date"] = pd.to_datetime(prices_df["date"])

# Filter rice products (category = "Arroz blanco")
rice_product_ids = products_df[products_df["category"] == "Arroz blanco"][
    "product_id"
].tolist()
print(f"Found {len(rice_product_ids)} rice products: {rice_product_ids}")

# Filter prices for rice
rice_prices = prices_df[prices_df["product_id"].isin(rice_product_ids)].copy()

print(f"Total rice price records: {len(rice_prices):,}")

rice_prices["year"] = rice_prices["date"].dt.year

# Aggregate by year (average across all rice products)
yearly_agg = (
    rice_prices.groupby("year")
    .agg(
        {
            "price_avg": "mean",
            "price_median": "mean",
            "price_min": "min",
            "price_max": "max",
        }
    )
    .reset_index()
)

print("\nYearly aggregated data (V4 IMPROVED CLEAN):")
print(yearly_agg)

# Create the visualization
fig, ax = plt.subplots(figsize=(14, 8))

# Plot min-max range as shaded area
ax.fill_between(
    yearly_agg["year"],
    yearly_agg["price_min"],
    yearly_agg["price_max"],
    alpha=0.2,
    color="steelblue",
    label="Range (Min-Max)",
)

# Plot average price
ax.plot(
    yearly_agg["year"],
    yearly_agg["price_avg"],
    marker="o",
    linewidth=2.5,
    markersize=8,
    color="darkblue",
    label="Average Price",
)

# Plot median price
ax.plot(
    yearly_agg["year"],
    yearly_agg["price_median"],
    marker="s",
    linewidth=2,
    markersize=7,
    color="orange",
    linestyle="--",
    label="Median Price",
)

# Formatting
ax.set_xlabel("Year", fontsize=12, fontweight="bold")
ax.set_ylabel("Price (UY$)", fontsize=12, fontweight="bold")
ax.set_title(
    "Rice Price Evolution in Uruguay (2016-2025)\nAll white rice products - Clean data (IQR)",
    fontsize=14,
    fontweight="bold",
    pad=20,
)

# Set x-axis to show all years
ax.set_xticks(yearly_agg["year"])

# Add grid
ax.grid(True, alpha=0.3)

# Legend
ax.legend(loc="upper left", fontsize=10, framealpha=0.9)

# Add value annotations for the most recent year
last_year = yearly_agg.iloc[-1]
ax.annotate(
    f"${last_year['price_avg']:.2f}",
    xy=(last_year["year"], last_year["price_avg"]),
    xytext=(10, 0),
    textcoords="offset points",
    fontsize=9,
    color="darkblue",
    fontweight="bold",
)

ax.annotate(
    f"${last_year['price_median']:.2f}",
    xy=(last_year["year"], last_year["price_median"]),
    xytext=(10, 0),
    textcoords="offset points",
    fontsize=9,
    color="orange",
    fontweight="bold",
)

# Calculate percentage increase
first_avg = yearly_agg.iloc[0]["price_avg"]
last_avg = yearly_agg.iloc[-1]["price_avg"]
pct_increase = ((last_avg - first_avg) / first_avg) * 100

# Add summary text box
textstr = f"Aumento total: {pct_increase:.1f}%\n"
textstr += f"2016 price: ${first_avg:.2f}\n"
textstr += f"2025 price: ${last_avg:.2f}\n"
textstr += f"Records: {len(rice_prices):,}\n"
textstr += "Dataset: IQR clean"

props = dict(boxstyle="round", facecolor="lightcyan", alpha=0.85)
ax.text(
    0.02,
    0.98,
    textstr,
    transform=ax.transAxes,
    fontsize=10,
    verticalalignment="top",
    bbox=props,
)

plt.tight_layout()

# Save the plot
output_path = Path(__file__).parent.parent / "outputs" / "rice_price_evolution_v4.png"
plt.savefig(output_path, dpi=300, bbox_inches="tight")
print(f"\nChart saved to: {output_path}")

if os.getenv("SHOW_PLOTS") == "1":
    plt.show()
