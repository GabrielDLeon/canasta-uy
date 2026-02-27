package uy.eleven.canasta.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Representa un rango de fechas.
 */
@Schema(description = "Rango de fechas para consultas")
public record DateRange(
        @Schema(description = "Fecha de inicio (ISO-8601)", example = "2024-01-01")
        LocalDate from, 
        
        @Schema(description = "Fecha de fin (ISO-8601)", example = "2024-12-31")
        LocalDate to) {}
