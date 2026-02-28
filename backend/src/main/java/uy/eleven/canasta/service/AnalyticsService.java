package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import uy.eleven.canasta.dto.analytics.ComparisonResponse;
import uy.eleven.canasta.dto.analytics.InflationResponse;
import uy.eleven.canasta.dto.analytics.TopChangesResponse;
import uy.eleven.canasta.dto.analytics.TrendResponse;
import uy.eleven.canasta.dto.common.DateRange;
import uy.eleven.canasta.exception.ProductNotFoundException;
import uy.eleven.canasta.exception.CategoryNotFoundException;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.CategoryRepository;
import uy.eleven.canasta.repository.PriceRepository;
import uy.eleven.canasta.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AnalyticsService {

    private static final int MAX_DATE_RANGE_DAYS = 365;
    private static final int DEFAULT_DATE_RANGE_DAYS = 365;

    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PriceService priceService;

    @Cacheable(
            value = "analytics",
            key = "'trend:' + #productId + ':' + #from + ':' + #to + ':' + #includeData",
            unless = "#result == null")
    public TrendResponse calculateTrend(
            Integer productId, LocalDate from, LocalDate to, boolean includeData) {

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        priceService.validateDateRange(effectiveFrom, effectiveTo);

        List<Price> prices =
                priceRepository.findByIdProductIdAndIdDateBetween(
                        productId, effectiveFrom, effectiveTo);

        if (prices.isEmpty()) {
            return new TrendResponse(
                    productId,
                    product.getName(),
                    new DateRange(effectiveFrom, effectiveTo),
                    new TrendResponse.TrendSummary(
                            "stable",
                            "flat",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "low"),
                    includeData ? List.of() : null);
        }

        prices.sort(Comparator.comparing(p -> p.getId().getDate()));

        Price firstPrice = prices.get(0);
        Price lastPrice = prices.get(prices.size() - 1);

        BigDecimal priceStart = firstPrice.getPriceAverage();
        BigDecimal priceEnd = lastPrice.getPriceAverage();

        BigDecimal variationAbsolute = priceEnd.subtract(priceStart);
        BigDecimal variationPercentage =
                priceStart.compareTo(BigDecimal.ZERO) != 0
                        ? variationAbsolute
                                .multiply(BigDecimal.valueOf(100))
                                .divide(priceStart, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        BigDecimal avgPrice =
                prices.stream()
                        .map(Price::getPriceAverage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minPrice =
                prices.stream()
                        .map(Price::getPriceAverage)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice =
                prices.stream()
                        .map(Price::getPriceAverage)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

        double mean =
                prices.stream()
                        .mapToDouble(p -> p.getPriceAverage().doubleValue())
                        .average()
                        .orElse(0.0);

        double variance =
                prices.stream()
                        .mapToDouble(p -> Math.pow(p.getPriceAverage().doubleValue() - mean, 2))
                        .average()
                        .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = mean != 0 ? (stdDev / mean) * 100 : 0;

        String volatility;
        if (coefficientOfVariation < 5) {
            volatility = "low";
        } else if (coefficientOfVariation < 15) {
            volatility = "medium";
        } else {
            volatility = "high";
        }

        String trend;
        String trendDirection;
        if (variationPercentage.compareTo(new BigDecimal("2")) > 0) {
            trend = "increasing";
            trendDirection = "up";
        } else if (variationPercentage.compareTo(new BigDecimal("-2")) < 0) {
            trend = "decreasing";
            trendDirection = "down";
        } else {
            trend = "stable";
            trendDirection = "flat";
        }

        List<TrendResponse.PricePoint> data = null;
        if (includeData) {
            data =
                    prices.stream()
                            .map(
                                    p ->
                                            new TrendResponse.PricePoint(
                                                    p.getId().getDate(),
                                                    p.getPriceAverage(),
                                                    p.getPriceMinimum(),
                                                    p.getPriceMaximum()))
                            .toList();
        }

        return new TrendResponse(
                productId,
                product.getName(),
                new DateRange(effectiveFrom, effectiveTo),
                new TrendResponse.TrendSummary(
                        trend,
                        trendDirection,
                        variationPercentage,
                        variationAbsolute,
                        priceStart,
                        priceEnd,
                        avgPrice,
                        minPrice,
                        maxPrice,
                        volatility),
                data);
    }

    @Cacheable(
            value = "analytics",
            key = "'inflation:' + #categoryId + ':' + #from + ':' + #to + ':' + #includeData",
            unless = "#result == null")
    public InflationResponse calculateInflation(
            Integer categoryId, LocalDate from, LocalDate to, boolean includeData) {

        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        priceService.validateDateRange(effectiveFrom, effectiveTo);

        List<Product> products = productRepository.findByCategory(category);

        if (products.isEmpty()) {
            return new InflationResponse(
                    categoryId,
                    category.getName(),
                    new DateRange(effectiveFrom, effectiveTo),
                    new InflationResponse.InflationSummary(
                            BigDecimal.ZERO, BigDecimal.ZERO, 0, "yearly"),
                    includeData ? List.of() : null);
        }

        Map<YearMonth, List<Price>> pricesByMonth =
                products.stream()
                        .flatMap(
                                p ->
                                        priceService
                                                .getPricesByProductAndDateRange(
                                                        p.getProductId(),
                                                        effectiveFrom,
                                                        effectiveTo)
                                                .stream())
                        .collect(Collectors.groupingBy(p -> YearMonth.from(p.getId().getDate())));

        List<YearMonth> sortedMonths =
                pricesByMonth.keySet().stream().sorted().collect(Collectors.toList());

        if (sortedMonths.isEmpty()) {
            return new InflationResponse(
                    categoryId,
                    category.getName(),
                    new DateRange(effectiveFrom, effectiveTo),
                    new InflationResponse.InflationSummary(
                            BigDecimal.ZERO, BigDecimal.ZERO, products.size(), "yearly"),
                    includeData ? List.of() : null);
        }

        List<InflationResponse.MonthlyInflation> monthlyData = new ArrayList<>();
        BigDecimal firstMonthAvg = null;
        BigDecimal lastMonthAvg = null;

        for (YearMonth month : sortedMonths) {
            List<Price> monthPrices = pricesByMonth.get(month);
            BigDecimal avgPrice =
                    monthPrices.stream()
                            .map(Price::getPriceAverage)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(
                                    BigDecimal.valueOf(monthPrices.size()),
                                    2,
                                    RoundingMode.HALF_UP);

            if (firstMonthAvg == null) {
                firstMonthAvg = avgPrice;
            }
            lastMonthAvg = avgPrice;

            if (includeData) {
                monthlyData.add(
                        new InflationResponse.MonthlyInflation(
                                month.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                                BigDecimal.ZERO,
                                avgPrice,
                                monthPrices.size()));
            }
        }

        for (int i = 1; i < monthlyData.size(); i++) {
            InflationResponse.MonthlyInflation current = monthlyData.get(i);
            InflationResponse.MonthlyInflation previous = monthlyData.get(i - 1);

            if (previous.avgPrice().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal monthlyInflation =
                        current.avgPrice()
                                .subtract(previous.avgPrice())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(previous.avgPrice(), 2, RoundingMode.HALF_UP);

                monthlyData.set(
                        i,
                        new InflationResponse.MonthlyInflation(
                                current.yearMonth(),
                                monthlyInflation,
                                current.avgPrice(),
                                current.dataPoints()));
            }
        }

        BigDecimal totalInflation =
                firstMonthAvg != null
                                && lastMonthAvg != null
                                && firstMonthAvg.compareTo(BigDecimal.ZERO) != 0
                        ? lastMonthAvg
                                .subtract(firstMonthAvg)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(firstMonthAvg, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        long monthsBetween =
                ChronoUnit.MONTHS.between(
                        sortedMonths.get(0), sortedMonths.get(sortedMonths.size() - 1));
        BigDecimal annualizedInflation =
                monthsBetween > 0
                        ? totalInflation
                                .multiply(BigDecimal.valueOf(12))
                                .divide(BigDecimal.valueOf(monthsBetween), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        return new InflationResponse(
                categoryId,
                category.getName(),
                new DateRange(effectiveFrom, effectiveTo),
                new InflationResponse.InflationSummary(
                        totalInflation,
                        annualizedInflation,
                        products.size(),
                        monthsBetween >= 12 ? "yearly" : "monthly"),
                includeData ? monthlyData : null);
    }

    @Cacheable(
            value = "analytics",
            key = "'compare:' + #productIds.hashCode() + ':' + #from + ':' + #to")
    public ComparisonResponse compareProducts(
            List<Integer> productIds, LocalDate from, LocalDate to) {

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        priceService.validateDateRange(effectiveFrom, effectiveTo);

        List<ComparisonResponse.ProductComparison> productComparisons = new ArrayList<>();

        for (Integer productId : productIds) {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                continue;
            }

            Product product = productOpt.get();
            List<Price> prices =
                    priceRepository.findByIdProductIdAndIdDateBetween(
                            productId, effectiveFrom, effectiveTo);

            if (prices.isEmpty()) {
                continue;
            }

            prices.sort(Comparator.comparing(p -> p.getId().getDate()));

            BigDecimal avgPrice =
                    prices.stream()
                            .map(Price::getPriceAverage)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

            BigDecimal minPrice =
                    prices.stream()
                            .map(Price::getPriceMinimum)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

            BigDecimal maxPrice =
                    prices.stream()
                            .map(Price::getPriceMaximum)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

            Price firstPrice = prices.get(0);
            Price lastPrice = prices.get(prices.size() - 1);
            BigDecimal variationPercentage =
                    firstPrice.getPriceAverage().compareTo(BigDecimal.ZERO) != 0
                            ? lastPrice
                                    .getPriceAverage()
                                    .subtract(firstPrice.getPriceAverage())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(firstPrice.getPriceAverage(), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            productComparisons.add(
                    new ComparisonResponse.ProductComparison(
                            productId,
                            product.getName(),
                            product.getCategory() != null
                                    ? product.getCategory().getName()
                                    : "Unknown",
                            avgPrice,
                            minPrice,
                            maxPrice,
                            variationPercentage,
                            prices.size()));
        }

        if (productComparisons.isEmpty()) {
            return new ComparisonResponse(
                    new DateRange(effectiveFrom, effectiveTo), List.of(), null);
        }

        ComparisonResponse.ProductComparison mostExpensive =
                productComparisons.stream()
                        .max(Comparator.comparing(ComparisonResponse.ProductComparison::avgPrice))
                        .orElse(null);

        ComparisonResponse.ProductComparison cheapest =
                productComparisons.stream()
                        .min(Comparator.comparing(ComparisonResponse.ProductComparison::avgPrice))
                        .orElse(null);

        ComparisonResponse.ProductComparison mostVolatile =
                productComparisons.stream()
                        .max(
                                Comparator.comparing(
                                        ComparisonResponse.ProductComparison::variationPercentage,
                                        Comparator.comparing(BigDecimal::abs)))
                        .orElse(null);

        BigDecimal priceDifference =
                mostExpensive != null && cheapest != null
                        ? mostExpensive.avgPrice().subtract(cheapest.avgPrice())
                        : BigDecimal.ZERO;

        BigDecimal priceRatio =
                mostExpensive != null
                                && cheapest != null
                                && cheapest.avgPrice().compareTo(BigDecimal.ZERO) != 0
                        ? mostExpensive
                                .avgPrice()
                                .divide(cheapest.avgPrice(), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        ComparisonResponse.ComparisonStats stats =
                new ComparisonResponse.ComparisonStats(
                        priceDifference,
                        priceRatio,
                        mostExpensive != null ? mostExpensive.productName() : "",
                        cheapest != null ? cheapest.productName() : "",
                        mostVolatile != null ? mostVolatile.productName() : "");

        return new ComparisonResponse(
                new DateRange(effectiveFrom, effectiveTo), productComparisons, stats);
    }

    @Cacheable(
            value = "analytics",
            key = "'top-changes:' + #period + ':' + #type + ':' + #limit + ':' + #categoryId")
    public TopChangesResponse getTopChanges(
            String period, String type, Integer limit, Integer categoryId) {

        LocalDate effectiveTo = LocalDate.now();
        LocalDate effectiveFrom;

        switch (period.toLowerCase()) {
            case "7d":
                effectiveFrom = effectiveTo.minusDays(7);
                break;
            case "90d":
                effectiveFrom = effectiveTo.minusDays(90);
                break;
            case "1y":
                effectiveFrom = effectiveTo.minusYears(1);
                break;
            case "30d":
            default:
                effectiveFrom = effectiveTo.minusDays(30);
                break;
        }

        List<Product> products;
        if (categoryId != null) {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return new TopChangesResponse(
                        period, new DateRange(effectiveFrom, effectiveTo), List.of());
            }
            products = productRepository.findByCategory(categoryOpt.get());
        } else {
            products = productRepository.findAll();
        }

        List<TopChangesResponse.PriceChange> changes = new ArrayList<>();

        for (Product product : products) {
            List<Price> prices =
                    priceRepository.findByIdProductIdAndIdDateBetween(
                            product.getProductId(), effectiveFrom, effectiveTo);

            if (prices.size() < 2) {
                continue;
            }

            prices.sort(Comparator.comparing(p -> p.getId().getDate()));

            Price firstPrice = prices.get(0);
            Price lastPrice = prices.get(prices.size() - 1);

            BigDecimal priceBefore = firstPrice.getPriceAverage();
            BigDecimal priceAfter = lastPrice.getPriceAverage();

            BigDecimal changeAbsolute = priceAfter.subtract(priceBefore);
            BigDecimal changePercentage =
                    priceBefore.compareTo(BigDecimal.ZERO) != 0
                            ? changeAbsolute
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(priceBefore, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            String changeDirection =
                    changePercentage.compareTo(BigDecimal.ZERO) > 0
                            ? "increase"
                            : changePercentage.compareTo(BigDecimal.ZERO) < 0
                                    ? "decrease"
                                    : "stable";

            if (!"all".equalsIgnoreCase(type)) {
                if ("increase".equalsIgnoreCase(type)
                        && changePercentage.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if ("decrease".equalsIgnoreCase(type)
                        && changePercentage.compareTo(BigDecimal.ZERO) >= 0) {
                    continue;
                }
            }

            changes.add(
                    new TopChangesResponse.PriceChange(
                            product.getProductId(),
                            product.getName(),
                            product.getCategory() != null
                                    ? product.getCategory().getName()
                                    : "Unknown",
                            priceBefore,
                            priceAfter,
                            changePercentage,
                            changeAbsolute,
                            changeDirection));
        }

        changes.sort((a, b) -> b.changePercentage().abs().compareTo(a.changePercentage().abs()));

        int resultLimit = Math.min(limit, changes.size());
        List<TopChangesResponse.PriceChange> topChanges = changes.subList(0, resultLimit);

        return new TopChangesResponse(
                period, new DateRange(effectiveFrom, effectiveTo), topChanges);
    }
}
