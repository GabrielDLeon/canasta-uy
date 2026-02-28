"""
Top price changes over a period using V4 clean data.
"""

from pathlib import Path
import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


def main() -> None:
    sns.set_theme(style="whitegrid")
    plt.rcParams["figure.figsize"] = (12, 8)

    data_path = (
        Path(__file__).parent.parent
        / "data"
        / "processed"
        / "prices_aggregated_all_years_v4.csv"
    )
    products_path = (
        Path(__file__).parent.parent / "data" / "processed" / "products_catalog.csv"
    )

    prices_df = pd.read_csv(data_path)
    products_df = pd.read_csv(products_path)

    prices_df["date"] = pd.to_datetime(prices_df["date"])

    min_date = prices_df["date"].min()
    max_date = prices_df["date"].max()

    start = min_date
    end = max_date

    first = prices_df.loc[
        prices_df["date"] == start, ["product_id", "price_avg"]
    ].copy()
    first.columns = ["product_id", "price_start"]
    last = prices_df.loc[prices_df["date"] == end, ["product_id", "price_avg"]].copy()
    last.columns = ["product_id", "price_end"]

    merged = first.merge(last, on="product_id", how="inner")
    merged = merged[merged["price_start"] > 0]
    merged["change_pct"] = (
        (merged["price_end"] - merged["price_start"]) / merged["price_start"]
    ) * 100

    merged = merged.merge(
        products_df[["product_id", "name", "category"]],
        on="product_id",
        how="left",
    )

    top = merged.sort_values("change_pct", ascending=False).head(15)
    top = top.iloc[::-1]

    fig, ax = plt.subplots()
    bars = ax.barh(
        top["name"],
        top["change_pct"],
        color="#ed8936",
        edgecolor="black",
        alpha=0.85,
    )
    ax.set_xlabel("Price Change (%)")
    ax.set_title(
        f"Top Price Changes ({start.date()} to {end.date()})",
        pad=12,
    )

    for bar in bars:
        value = bar.get_width()
        ax.text(
            value + 1, bar.get_y() + bar.get_height() / 2, f"{value:.1f}%", va="center"
        )

    ax.grid(True, axis="x", alpha=0.3)
    plt.tight_layout()

    output_path = Path(__file__).parent.parent / "outputs" / "top_price_changes.png"
    plt.savefig(output_path, dpi=300, bbox_inches="tight")
    print(f"Chart saved to: {output_path}")
    if os.getenv("SHOW_PLOTS") == "1":
        plt.show()


if __name__ == "__main__":
    main()
