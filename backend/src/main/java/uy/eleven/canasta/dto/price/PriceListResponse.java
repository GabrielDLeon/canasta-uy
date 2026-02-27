package uy.eleven.canasta.dto.price;

import io.swagger.v3.oas.annotations.media.Schema;

import uy.eleven.canasta.dto.common.DateRange;

import java.util.List;

/**
 * Respuesta con lista de precios de un producto.
 */
@Schema(description = "Lista de precios históricos de un producto")
public record PriceListResponse(
        @Schema(description = "ID del producto", example = "15")
        Integer productId,
        
        @Schema(description = "Nombre del producto", example = "Arroz blanco Green Chef 1kg")
        String productName,
        
        @Schema(description = "Rango de fechas de los datos")
        DateRange period,
        
        @Schema(description = "Granularidad de los datos (daily, weekly, monthly)", example = "daily")
        String granularity,
        
        @Schema(description = "Lista de registros de precios")
        List<PriceResponse> prices) {}
