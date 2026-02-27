package uy.eleven.canasta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Operaciones para consultar el catálogo de productos")
public class ProductController {
    private final ProductService productService;

    @Operation(
            summary = "Listar todos los productos",
            description = "Obtiene una lista paginada de todos los productos disponibles en el catálogo")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lista de productos obtenida exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getAllProducts(
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página", example = "20")
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

    @Operation(
            summary = "Buscar productos por nombre",
            description = "Busca productos que coincidan con el término de búsqueda proporcionado")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Búsqueda completada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ProductListResponse>> searchProductsByName(
            @Parameter(description = "Término de búsqueda", example = "Arroz", required = true)
            @RequestParam String query,
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página", example = "20")
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

    @Operation(
            summary = "Obtener producto por ID",
            description = "Obtiene los detalles de un producto específico por su ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Producto encontrado",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Producto no encontrado",
                content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(
            @Parameter(description = "ID del producto", example = "15", required = true)
            @PathVariable Integer id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
