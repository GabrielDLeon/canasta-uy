package uy.eleven.canasta.dto.category;

import uy.eleven.canasta.dto.common.PaginationInfo;

import java.util.List;

public record CategoryProductsResponse(
        Integer categoryId,
        String categoryName,
        List<ProductSummary> products,
        PaginationInfo pagination) {
    public record ProductSummary(Integer id, String name, String brand, String specification) {}
}
