package uy.eleven.canasta.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record ComparisonResponse(
        DateRange period, List<ProductComparison> products, ComparisonStats comparison)
        implements java.io.Serializable {
    public record ProductComparison(
            Integer productId,
            String productName,
            String category,
            BigDecimal avgPrice,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal variationPercentage,
            Integer dataPoints,
            List<PricePoint> data) {}

    public record PricePoint(LocalDate date, BigDecimal priceAvg) {}

    public record ComparisonStats(
            BigDecimal priceDifference,
            BigDecimal priceRatio,
            String mostExpensiveProduct,
            String cheapestProduct,
            String mostVolatileProduct) {}
}
