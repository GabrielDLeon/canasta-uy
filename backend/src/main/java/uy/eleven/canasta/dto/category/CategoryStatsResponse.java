package uy.eleven.canasta.dto.category;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;

public record CategoryStatsResponse(
        Integer categoryId,
        String categoryName,
        Integer productCount,
        DateRange period,
        PriceStats stats) {
    public record PriceStats(
            BigDecimal avgPrice,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal medianPrice) {}
}
