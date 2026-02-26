package uy.eleven.canasta.dto.product;

import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.model.Product;

import java.util.List;

public record ProductListResponse(List<Product> products, PaginationInfo pagination) {}
