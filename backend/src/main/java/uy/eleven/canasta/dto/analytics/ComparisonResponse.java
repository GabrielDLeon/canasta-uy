package uy.eleven.canasta.dto.analytics;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

public record ComparisonResponse(
        DateRange period, List<ProductComparison> products, ComparisonStats comparison) {
    public record ProductComparison(
            Integer productId,
            String productName,
            String category,
            BigDecimal avgPrice,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal variationPercentage,
            Integer dataPoints) {}

    public record ComparisonStats(
            BigDecimal priceDifference,
            BigDecimal priceRatio,
            String mostExpensiveProduct,
            String cheapestProduct,
            String mostVolatileProduct) {}
}
