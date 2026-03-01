package uy.eleven.canasta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.dto.analytics.ComparisonResponse;
import uy.eleven.canasta.dto.analytics.InflationResponse;
import uy.eleven.canasta.dto.analytics.TopChangesResponse;
import uy.eleven.canasta.dto.analytics.TrendResponse;
import uy.eleven.canasta.service.AnalyticsService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
@AllArgsConstructor
@Tag(name = "Analytics", description = "Endpoints de análisis y métricas de precios")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Obtener tendencia de precio",
            description = "Analiza la tendencia de precios de un producto en un rango de fechas")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tendencia calculada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Producto no encontrado",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Rango de fechas inválido (máximo 365 días)",
                content = @Content)
    })
    @GetMapping("/trend/{productId}")
    public ResponseEntity<ApiResponse<TrendResponse>> getTrend(
            @Parameter(description = "ID del producto", example = "1", required = true)
                    @PathVariable
                    Integer productId,
            @Parameter(description = "Fecha inicio (YYYY-MM-DD)", example = "2024-01-01")
                    @RequestParam(required = false)
                    LocalDate from,
            @Parameter(description = "Fecha fin (YYYY-MM-DD)", example = "2024-12-31")
                    @RequestParam(required = false)
                    LocalDate to,
            @Parameter(description = "Incluir datos raw", example = "false")
                    @RequestParam(defaultValue = "false")
                    boolean includeData) {

        TrendResponse response = analyticsService.calculateTrend(productId, from, to, includeData);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Obtener inflación por categoría",
            description = "Calcula la tasa de inflación para todos los productos de una categoría")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Inflación calculada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Categoría no encontrada",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Rango de fechas inválido",
                content = @Content)
    })
    @GetMapping("/inflation/{categoryId}")
    public ResponseEntity<ApiResponse<InflationResponse>> getInflation(
            @Parameter(description = "ID de la categoría", example = "1", required = true)
                    @PathVariable
                    Integer categoryId,
            @Parameter(description = "Fecha inicio (YYYY-MM-DD)", example = "2024-01-01")
                    @RequestParam(required = false)
                    LocalDate from,
            @Parameter(description = "Fecha fin (YYYY-MM-DD)", example = "2024-12-31")
                    @RequestParam(required = false)
                    LocalDate to,
            @Parameter(description = "Incluir desglose mensual", example = "false")
                    @RequestParam(defaultValue = "false")
                    boolean includeData) {

        InflationResponse response =
                analyticsService.calculateInflation(categoryId, from, to, includeData);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Comparar productos",
            description = "Compara precios y estadísticas entre múltiples productos")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Comparación realizada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Parámetros inválidos (máximo 5 productos, rango 365 días)",
                content = @Content)
    })
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<ComparisonResponse>> compareProducts(
            @Parameter(
                            description = "IDs de productos separados por coma (2-5 productos)",
                            example = "1,2,3",
                            required = true)
                    @RequestParam
                    String productIds,
            @Parameter(description = "Fecha inicio (YYYY-MM-DD)", example = "2024-01-01")
                    @RequestParam(required = false)
                    LocalDate from,
            @Parameter(description = "Fecha fin (YYYY-MM-DD)", example = "2024-12-31")
                    @RequestParam(required = false)
                    LocalDate to,
            @Parameter(description = "Incluir serie diaria de precios (date + priceAvg)")
                    @RequestParam(defaultValue = "false")
                    boolean includeData) {

        List<Integer> ids =
                Arrays.stream(productIds.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

        if (ids.size() < 2 || ids.size() > 5) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Se requieren entre 2 y 5 productos para comparar"));
        }

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(365);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        ComparisonResponse response =
                analyticsService.compareProducts(ids, effectiveFrom, effectiveTo, includeData);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Top variaciones de precios",
            description = "Obtiene los productos con mayores variaciones de precio en un período")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Variaciones obtenidas exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/top-changes")
    public ResponseEntity<ApiResponse<TopChangesResponse>> getTopChanges(
            @Parameter(
                            description = "Período",
                            example = "30d",
                            schema = @Schema(allowableValues = {"7d", "30d", "90d", "1y"}))
                    @RequestParam(defaultValue = "30d")
                    String period,
            @Parameter(
                            description = "Tipo de cambio",
                            example = "all",
                            schema = @Schema(allowableValues = {"increase", "decrease", "all"}))
                    @RequestParam(defaultValue = "all")
                    String type,
            @Parameter(description = "Cantidad de resultados (máx 50)", example = "10")
                    @RequestParam(defaultValue = "10")
                    int limit,
            @Parameter(description = "Filtrar por categoría", example = "1")
                    @RequestParam(required = false)
                    Integer categoryId) {

        int effectiveLimit = Math.min(limit, 50);

        TopChangesResponse response =
                analyticsService.getTopChanges(period, type, effectiveLimit, categoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
