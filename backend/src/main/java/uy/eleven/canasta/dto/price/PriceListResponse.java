package uy.eleven.canasta.dto.price;

import uy.eleven.canasta.dto.common.DateRange;

import java.util.List;

public record PriceListResponse(
        Integer productId,
        String productName,
        DateRange period,
        String granularity,
        List<PriceResponse> prices) {}
