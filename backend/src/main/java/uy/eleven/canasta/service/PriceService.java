package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import uy.eleven.canasta.dto.common.DateRange;
import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.dto.price.PriceListResponse;
import uy.eleven.canasta.dto.price.PriceResponse;
import uy.eleven.canasta.dto.price.PriceSearchResponse;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.PriceRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PriceService {

    private static final int MAX_DATE_RANGE_DAYS = 365;
    private static final int DEFAULT_DATE_RANGE_DAYS = 365;

    private final PriceRepository priceRepository;
    private final ProductService productService;

    public void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("'from' date must be before 'to' date");
            }
            long days = ChronoUnit.DAYS.between(from, to);
            if (days > MAX_DATE_RANGE_DAYS) {
                throw new IllegalArgumentException(
                        "Date range cannot exceed " + MAX_DATE_RANGE_DAYS + " days");
            }
        }
    }

    public PriceListResponse getProductPriceListResponse(
            Integer productId, LocalDate from, LocalDate to, String granularity) {

        Product product = productService.getProductById(productId);

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        validateDateRange(effectiveFrom, effectiveTo);

        List<PriceResponse> priceResponses =
                priceRepository
                        .findByIdProductIdAndIdDateBetween(productId, effectiveFrom, effectiveTo)
                        .stream()
                        .map(this::mapToPriceResponse)
                        .toList();

        return new PriceListResponse(
                productId,
                product.getName(),
                new DateRange(effectiveFrom, effectiveTo),
                granularity,
                priceResponses);
    }

    public PriceSearchResponse searchPricesResponse(
            String productIds, LocalDate from, LocalDate to, int page, int size) {

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        validateDateRange(effectiveFrom, effectiveTo);

        List<Price> allPrices;
        if (productIds != null && !productIds.isEmpty()) {
            List<Integer> ids =
                    Arrays.stream(productIds.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
            allPrices =
                    priceRepository.findByIdDateBetween(effectiveFrom, effectiveTo).stream()
                            .filter(p -> ids.contains(p.getId().getProductId()))
                            .toList();
        } else {
            allPrices = priceRepository.findByIdDateBetween(effectiveFrom, effectiveTo);
        }

        List<PriceResponse> priceResponses =
                allPrices.stream()
                        .skip((long) page * size)
                        .limit(size)
                        .map(this::mapToPriceResponse)
                        .toList();

        long totalElements = allPrices.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PaginationInfo pagination =
                new PaginationInfo(
                        page, size, totalElements, totalPages, page < totalPages - 1, page > 0);

        return new PriceSearchResponse(priceResponses, pagination);
    }

    private PriceResponse mapToPriceResponse(Price price) {
        return new PriceResponse(
                price.getId().getDate(),
                price.getPriceMinimum(),
                price.getPriceMaximum(),
                price.getPriceAverage(),
                price.getPriceMedian(),
                price.getStoreCount(),
                price.getOfferPercentage());
    }

    // Método mantenido para compatibilidad con CategoryService
    public List<Price> getPricesByProductAndDateRange(
            Integer productId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return priceRepository.findByIdProductIdAndIdDateBetween(productId, startDate, endDate);
    }
}
