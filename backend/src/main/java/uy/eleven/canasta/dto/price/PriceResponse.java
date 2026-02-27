package uy.eleven.canasta.dto.price;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa un registro de precios para una fecha específica.
 */
@Schema(description = "Registro de precios de un producto para una fecha")
public record PriceResponse(
        @Schema(description = "Fecha del registro", example = "2024-01-15")
        LocalDate date,
        
        @Schema(description = "Precio mínimo observado", example = "45.50")
        BigDecimal priceMin,
        
        @Schema(description = "Precio máximo observado", example = "52.99")
        BigDecimal priceMax,
        
        @Schema(description = "Precio promedio", example = "48.25")
        BigDecimal priceAvg,
        
        @Schema(description = "Precio mediano", example = "47.80")
        BigDecimal priceMedian,
        
        @Schema(description = "Cantidad de tiendas que reportaron", example = "12")
        Integer storeCount,
        
        @Schema(description = "Porcentaje de tiendas con ofertas", example = "25.0")
        BigDecimal offerPercentage) {}
