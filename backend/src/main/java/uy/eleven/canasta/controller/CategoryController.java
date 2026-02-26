package uy.eleven.canasta.controller;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.dto.category.CategoryProductsRequest;
import uy.eleven.canasta.dto.category.CategoryProductsResponse;
import uy.eleven.canasta.dto.category.CategoryStatsRequest;
import uy.eleven.canasta.dto.category.CategoryStatsResponse;
import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.service.CategoryService;

@RestController
@RequestMapping("/api/v1/categories")
@AllArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<CategoryProductsResponse>> getCategoryProducts(
            @PathVariable Integer id, @Valid @ModelAttribute CategoryProductsRequest request) {

        CategoryProductsResponse response =
                categoryService.getCategoryProductsResponse(
                        id, PageRequest.of(request.page(), request.size()));

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        Page<Product> productPage =
                categoryService.getProductsByCategory(
                        id, PageRequest.of(request.page(), request.size()));

        PaginationInfo pagination =
                new PaginationInfo(
                        request.page(),
                        request.size(),
                        productPage.getTotalElements(),
                        productPage.getTotalPages(),
                        productPage.hasNext(),
                        productPage.hasPrevious());

        CategoryProductsResponse responseWithPagination =
                new CategoryProductsResponse(
                        response.categoryId(),
                        response.categoryName(),
                        response.products(),
                        pagination);

        return ResponseEntity.ok(ApiResponse.success(responseWithPagination));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<CategoryStatsResponse>> getCategoryStats(
            @PathVariable Integer id, @Valid @ModelAttribute CategoryStatsRequest request) {

        CategoryStatsResponse response =
                categoryService.calculateCategoryStats(
                        id, request.getEffectiveFrom(), request.getEffectiveTo());

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
