package uy.eleven.canasta.dto.analytics;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

public record TopChangesResponse(String period, DateRange dateRange, List<PriceChange> changes) {
    public record PriceChange(
            Integer productId,
            String productName,
            String category,
            BigDecimal priceBefore,
            BigDecimal priceAfter,
            BigDecimal changePercentage,
            BigDecimal changeAbsolute,
            String changeDirection) {}
}
