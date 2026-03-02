package uy.eleven.canasta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.dto.category.CategoryProductsRequest;
import uy.eleven.canasta.dto.category.CategoryProductsResponse;
import uy.eleven.canasta.dto.category.CategoryListResponse;
import uy.eleven.canasta.dto.category.CategoryStatsRequest;
import uy.eleven.canasta.dto.category.CategoryStatsResponse;
import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Operaciones para consultar categorías y estadísticas")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(
            summary = "Listar categorías",
            description = "Obtiene una lista paginada de categorías ordenadas alfabéticamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Categorías obtenidas exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryListResponse>> getAllCategories(
            @Parameter(description = "Número de página (0-based)", example = "0")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Cantidad de elementos por página", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size) {

        Page<Category> categoryPage = categoryService.getAllCategoriesPaginated(page, size);

        PaginationInfo pagination =
                new PaginationInfo(
                        categoryPage.getNumber(),
                        categoryPage.getSize(),
                        categoryPage.getTotalElements(),
                        categoryPage.getTotalPages(),
                        categoryPage.hasNext(),
                        categoryPage.hasPrevious());

        CategoryListResponse response =
                new CategoryListResponse(categoryPage.getContent(), pagination);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Buscar categorías por nombre",
            description = "Busca categorías por nombre y devuelve resultados paginados")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Búsqueda completada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<CategoryListResponse>> searchCategoriesByName(
            @Parameter(description = "Término de búsqueda", example = "Bebidas", required = true)
                    @RequestParam
                    String query,
            @Parameter(description = "Número de página (0-based)", example = "0")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Cantidad de elementos por página", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size) {

        Page<Category> categoryPage = categoryService.searchCategoriesByNamePaginated(query, page, size);

        PaginationInfo pagination =
                new PaginationInfo(
                        categoryPage.getNumber(),
                        categoryPage.getSize(),
                        categoryPage.getTotalElements(),
                        categoryPage.getTotalPages(),
                        categoryPage.hasNext(),
                        categoryPage.hasPrevious());

        CategoryListResponse response =
                new CategoryListResponse(categoryPage.getContent(), pagination);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Obtener productos de una categoría",
            description = "Obtiene la lista de productos pertenecientes a una categoría específica")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Productos obtenidos exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Categoría no encontrada",
                content = @Content)
    })
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<CategoryProductsResponse>> getCategoryProducts(
            @Parameter(description = "ID de la categoría", example = "1", required = true)
            @PathVariable Integer id, 
            @Valid @ModelAttribute CategoryProductsRequest request) {

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

    @Operation(
            summary = "Obtener estadísticas de una categoría",
            description = "Obtiene estadísticas de precios para todos los productos de una categoría en un rango de fechas")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Estadísticas obtenidas exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Categoría no encontrada",
                content = @Content)
    })
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<CategoryStatsResponse>> getCategoryStats(
            @Parameter(description = "ID de la categoría", example = "1", required = true)
            @PathVariable Integer id, 
            @Valid @ModelAttribute CategoryStatsRequest request) {

        CategoryStatsResponse response =
                categoryService.calculateCategoryStats(
                        id, request.getEffectiveFrom(), request.getEffectiveTo());

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
