package uy.eleven.canasta.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.model.Product;

import java.util.List;

/**
 * Respuesta con lista de productos y paginación.
 */
@Schema(description = "Lista paginada de productos")
public record ProductListResponse(
        @Schema(description = "Lista de productos")
        List<Product> products, 
        
        @Schema(description = "Información de paginación")
        PaginationInfo pagination) {}
