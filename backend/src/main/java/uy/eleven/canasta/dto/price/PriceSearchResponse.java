package uy.eleven.canasta.dto.price;

import uy.eleven.canasta.dto.common.PaginationInfo;

import java.util.List;

public record PriceSearchResponse(List<PriceResponse> prices, PaginationInfo pagination) {}
