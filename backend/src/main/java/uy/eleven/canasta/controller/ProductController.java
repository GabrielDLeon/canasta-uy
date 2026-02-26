package uy.eleven.canasta.controller;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.dto.product.ProductListResponse;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.service.ProductService;

@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> productPage = productService.getAllProductsPaginated(page, size);

        PaginationInfo pagination =
                new PaginationInfo(
                        productPage.getNumber(),
                        productPage.getSize(),
                        productPage.getTotalElements(),
                        productPage.getTotalPages(),
                        productPage.hasNext(),
                        productPage.hasPrevious());

        ProductListResponse response =
                new ProductListResponse(productPage.getContent(), pagination);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ProductListResponse>> searchProductsByName(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> productPage = productService.searchProductsByNamePaginated(query, page, size);

        PaginationInfo pagination =
                new PaginationInfo(
                        productPage.getNumber(),
                        productPage.getSize(),
                        productPage.getTotalElements(),
                        productPage.getTotalPages(),
                        productPage.hasNext(),
                        productPage.hasPrevious());

        ProductListResponse response =
                new ProductListResponse(productPage.getContent(), pagination);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Integer id) {
        return productService
                .getProductById(id)
                .map(product -> ResponseEntity.ok(ApiResponse.success(product)))
                .orElse(ResponseEntity.notFound().build());
    }
}
