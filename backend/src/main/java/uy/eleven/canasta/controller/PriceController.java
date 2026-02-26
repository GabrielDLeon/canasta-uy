package uy.eleven.canasta.controller;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.dto.common.DateRange;
import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.dto.price.PriceListResponse;
import uy.eleven.canasta.dto.price.PriceResponse;
import uy.eleven.canasta.dto.price.PriceSearchRequest;
import uy.eleven.canasta.dto.price.PriceSearchResponse;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.service.PriceService;
import uy.eleven.canasta.service.ProductService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class PriceController {

    private final PriceService priceService;
    private final ProductService productService;

    @GetMapping("/products/{id}/prices")
    public ResponseEntity<ApiResponse<PriceListResponse>> getProductPrices(
            @PathVariable Integer id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "daily") String granularity) {

        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOpt.get();

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(365);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        List<PriceResponse> priceResponses =
                priceService.getPriceResponsesByProductAndDateRange(
                        id, effectiveFrom, effectiveTo);

        PriceListResponse response =
                new PriceListResponse(
                        id,
                        product.getName(),
                        new DateRange(effectiveFrom, effectiveTo),
                        granularity,
                        priceResponses);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<PriceSearchResponse>> searchPrices(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "daily") String granularity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(365);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        List<PriceResponse> allPrices;
        if (productIds != null && !productIds.isEmpty()) {
            List<Integer> ids =
                    Arrays.stream(productIds.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
            allPrices =
                    priceService.getPriceResponsesByProductIdsAndDateRange(
                            ids, effectiveFrom, effectiveTo);
        } else {
            allPrices =
                    priceService.getPriceResponsesByDateRange(
                            effectiveFrom, effectiveTo);
        }

        List<PriceResponse> priceResponses =
                allPrices.stream()
                        .skip((long) page * size)
                        .limit(size)
                        .toList();

        long totalElements = allPrices.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PaginationInfo pagination =
                new PaginationInfo(
                        page,
                        size,
                        totalElements,
                        totalPages,
                        page < totalPages - 1,
                        page > 0);

        PriceSearchResponse response = new PriceSearchResponse(priceResponses, pagination);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
