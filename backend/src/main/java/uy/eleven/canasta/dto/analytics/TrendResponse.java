package uy.eleven.canasta.dto.analytics;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrendResponse(
        Integer productId,
        String productName,
        DateRange period,
        TrendSummary summary,
        List<PricePoint> data) {
    public record TrendSummary(
            String trend,
            String trendDirection,
            BigDecimal variationPercentage,
            BigDecimal variationAbsolute,
            BigDecimal priceStart,
            BigDecimal priceEnd,
            BigDecimal priceAvg,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String volatility) {}

    public record PricePoint(
            LocalDate date, BigDecimal priceAvg, BigDecimal priceMin, BigDecimal priceMax) {}
}
