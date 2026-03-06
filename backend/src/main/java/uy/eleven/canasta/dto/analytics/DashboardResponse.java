package uy.eleven.canasta.dto.analytics;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        String period,
        DateRange dateRange,
        MarketSnapshot marketSnapshot,
        List<ProductChange> topIncreases,
        List<ProductChange> topDecreases,
        List<CategoryChange> categoryChanges,
        VolatilitySummary volatility) {
    public record MarketSnapshot(
            BigDecimal avgPriceCurrent,
            BigDecimal avgPricePrevious,
            BigDecimal changePercentage,
            BigDecimal changeAbsolute) {}

    public record ProductChange(
            Integer productId,
            String productName,
            String category,
            BigDecimal priceBefore,
            BigDecimal priceAfter,
            BigDecimal changePercentage,
            BigDecimal changeAbsolute,
            String changeDirection) {}

    public record CategoryChange(
            String category,
            BigDecimal avgChangePercentage,
            BigDecimal avgChangeAbsolute,
            Integer productsCount) {}

    public record VolatilitySummary(List<VolatilityItem> mostVolatile) {}

    public record VolatilityItem(
            Integer productId, String productName, String category, BigDecimal coefficientOfVariation) {}
}
