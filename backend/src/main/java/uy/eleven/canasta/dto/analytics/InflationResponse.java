package uy.eleven.canasta.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record InflationResponse(
        Integer categoryId,
        String categoryName,
        DateRange period,
        InflationSummary summary,
        List<MonthlyInflation> data)
        implements java.io.Serializable {
    public record InflationSummary(
            BigDecimal totalInflationPercentage,
            BigDecimal annualizedInflation,
            Integer productsCount,
            String periodType) {}

    public record MonthlyInflation(
            String yearMonth,
            BigDecimal inflationPercentage,
            BigDecimal avgPrice,
            Integer dataPoints) {}
}
