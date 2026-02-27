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
import uy.eleven.canasta.dto.price.PriceListResponse;
import uy.eleven.canasta.dto.price.PriceSearchResponse;
import uy.eleven.canasta.service.PriceService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
@Tag(name = "Prices")
public class PriceController {

    private final PriceService priceService;

    @Operation(summary = "Get product prices")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")
    })
    @GetMapping("/products/{id}/prices")
    public ResponseEntity<ApiResponse<PriceListResponse>> getProductPrices(
            @Parameter @PathVariable Integer id,
            @Parameter @RequestParam(required = false) LocalDate from,
            @Parameter @RequestParam(required = false) LocalDate to,
            @Parameter @RequestParam(defaultValue = "daily") String granularity) {

        PriceListResponse response =
                priceService.getProductPriceListResponse(id, from, to, granularity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search prices")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<PriceSearchResponse>> searchPrices(
            @Parameter @RequestParam(required = false) String productIds,
            @Parameter @RequestParam(required = false) LocalDate from,
            @Parameter @RequestParam(required = false) LocalDate to,
            @Parameter @RequestParam(defaultValue = "0") int page,
            @Parameter @RequestParam(defaultValue = "20") int size) {

        PriceSearchResponse response =
                priceService.searchPricesResponse(productIds, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
