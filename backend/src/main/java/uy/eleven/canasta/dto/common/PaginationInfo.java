package uy.eleven.canasta.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Información de paginación para respuestas de lista.
 */
@Schema(description = "Información de paginación")
public record PaginationInfo(
        @Schema(description = "Número de página actual (0-based)", example = "0")
        int page,
        
        @Schema(description = "Cantidad de elementos por página", example = "20")
        int size,
        
        @Schema(description = "Total de elementos disponibles", example = "379")
        long totalElements,
        
        @Schema(description = "Total de páginas", example = "19")
        int totalPages,
        
        @Schema(description = "Indica si hay más páginas disponibles", example = "true")
        boolean hasNext,
        
        @Schema(description = "Indica si hay páginas anteriores", example = "false")
        boolean hasPrevious) {}
