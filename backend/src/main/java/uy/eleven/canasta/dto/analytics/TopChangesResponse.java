package uy.eleven.canasta.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uy.eleven.canasta.dto.common.DateRange;

import java.math.BigDecimal;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record TopChangesResponse(String period, DateRange dateRange, List<PriceChange> changes)
        implements java.io.Serializable {
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
