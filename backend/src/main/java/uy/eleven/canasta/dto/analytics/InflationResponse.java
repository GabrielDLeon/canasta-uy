package uy.eleven.canasta.dto.analytics;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

public record InflationResponse(
        Integer categoryId,
        String categoryName,
        DateRange period,
        InflationSummary summary,
        List<MonthlyInflation> data) {
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
