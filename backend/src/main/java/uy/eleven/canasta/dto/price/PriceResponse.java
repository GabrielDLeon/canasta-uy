package uy.eleven.canasta.dto.price;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
        LocalDate date,
        BigDecimal priceMin,
        BigDecimal priceMax,
        BigDecimal priceAvg,
        BigDecimal priceMedian,
        Integer storeCount,
        BigDecimal offerPercentage) {}
