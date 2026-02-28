package uy.eleven.canasta.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uy.eleven.canasta.dto.common.DateRange;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record TrendResponse(
        Integer productId,
        String productName,
        DateRange period,
        TrendSummary summary,
        List<PricePoint> data)
        implements Serializable {
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
