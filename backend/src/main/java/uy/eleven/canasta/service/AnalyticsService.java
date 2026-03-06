package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import uy.eleven.canasta.dto.analytics.ComparisonResponse;
import uy.eleven.canasta.dto.analytics.DashboardResponse;
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

        DateRange effectiveDateRange =
                resolveEffectiveDateRange(from, to, DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveFrom = effectiveDateRange.from();
        LocalDate effectiveTo = effectiveDateRange.to();

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

        DateRange effectiveDateRange =
                resolveEffectiveDateRange(from, to, DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveFrom = effectiveDateRange.from();
        LocalDate effectiveTo = effectiveDateRange.to();

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

    public ComparisonResponse compareProducts(
            List<Integer> productIds, LocalDate from, LocalDate to) {
        DateRange effectiveDateRange =
                resolveEffectiveDateRange(from, to, DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveFrom = effectiveDateRange.from();
        LocalDate effectiveTo = effectiveDateRange.to();
        return compareProducts(productIds, effectiveFrom, effectiveTo, false);
    }

    @Cacheable(
            value = "analytics",
            key =
                    "'compare:' + #productIds.hashCode() + ':' + #from + ':' + #to + ':'"
                            + " + #includeData")
    public ComparisonResponse compareProducts(
            List<Integer> productIds, LocalDate from, LocalDate to, boolean includeData) {

        DateRange effectiveDateRange = resolveEffectiveDateRange(from, to, DEFAULT_DATE_RANGE_DAYS);
        LocalDate effectiveFrom = effectiveDateRange.from();
        LocalDate effectiveTo = effectiveDateRange.to();

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

            List<ComparisonResponse.PricePoint> priceData =
                    includeData
                            ? prices.stream()
                                    .map(
                                            price ->
                                                    new ComparisonResponse.PricePoint(
                                                            price.getId().getDate(),
                                                            price.getPriceAverage()))
                                    .toList()
                            : null;

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
                            prices.size(),
                            priceData));
        }

        if (productComparisons.isEmpty()) {
            return new ComparisonResponse(effectiveDateRange, List.of(), null);
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

        return new ComparisonResponse(effectiveDateRange, productComparisons, stats);
    }

    @Cacheable(
            value = "analytics",
            key = "'top-changes:' + #period + ':' + #type + ':' + #limit + ':' + #categoryId")
    public TopChangesResponse getTopChanges(
            String period, String type, Integer limit, Integer categoryId) {

        LocalDate effectiveTo = resolveLatestAvailableDate();
        DateRange dateRange = resolveDateRangeByPeriod(period, effectiveTo);
        LocalDate effectiveFrom = dateRange.from();
        String normalizedType = type == null ? "all" : type.toLowerCase();

        List<ProductChangeMetric> changes = buildProductChanges(effectiveFrom, effectiveTo, categoryId);

        List<ProductChangeMetric> filtered =
                changes.stream()
                        .filter(
                                change -> {
                                    if ("increase".equals(normalizedType)) {
                                        return change.changePercentage().compareTo(BigDecimal.ZERO) > 0;
                                    }
                                    if ("decrease".equals(normalizedType)) {
                                        return change.changePercentage().compareTo(BigDecimal.ZERO) < 0;
                                    }
                                    return true;
                                })
                        .toList();

        List<ProductChangeMetric> sorted =
                filtered.stream()
                        .sorted(
                                (a, b) ->
                                        b.changePercentage()
                                                .abs()
                                                .compareTo(a.changePercentage().abs()))
                        .toList();

        int resultLimit = Math.min(limit, sorted.size());
        List<TopChangesResponse.PriceChange> topChanges =
                sorted.stream()
                        .limit(resultLimit)
                        .map(
                                change ->
                                        new TopChangesResponse.PriceChange(
                                                change.productId(),
                                                change.productName(),
                                                change.category(),
                                                change.priceBefore(),
                                                change.priceAfter(),
                                                change.changePercentage(),
                                                change.changeAbsolute(),
                                                change.changeDirection()))
                        .toList();

        return new TopChangesResponse(normalizePeriod(period), dateRange, topChanges);
    }

    @Cacheable(value = "analytics", key = "'dashboard:' + #period + ':' + #limit")
    public DashboardResponse getDashboardSummary(String period, Integer limit) {
        LocalDate effectiveTo = resolveLatestAvailableDate();
        DateRange dateRange = resolveDateRangeByPeriod(period, effectiveTo);
        LocalDate effectiveFrom = dateRange.from();
        int effectiveLimit = Math.max(1, Math.min(limit, 20));

        List<ProductChangeMetric> productChanges = buildProductChanges(effectiveFrom, effectiveTo, null);

        List<DashboardResponse.ProductChange> topIncreases =
                productChanges.stream()
                        .filter(change -> change.changePercentage().compareTo(BigDecimal.ZERO) > 0)
                        .sorted(
                                (a, b) ->
                                        b.changePercentage().compareTo(a.changePercentage()))
                        .limit(effectiveLimit)
                        .map(this::toDashboardProductChange)
                        .toList();

        List<DashboardResponse.ProductChange> topDecreases =
                productChanges.stream()
                        .filter(change -> change.changePercentage().compareTo(BigDecimal.ZERO) < 0)
                        .sorted(Comparator.comparing(ProductChangeMetric::changePercentage))
                        .limit(effectiveLimit)
                        .map(this::toDashboardProductChange)
                        .toList();

        List<DashboardResponse.CategoryChange> categoryChanges =
                buildCategoryChanges(productChanges, effectiveLimit);
        DashboardResponse.MarketSnapshot marketSnapshot =
                buildMarketSnapshot(effectiveFrom, effectiveTo);
        DashboardResponse.VolatilitySummary volatility =
                buildVolatilitySummary(effectiveFrom, effectiveTo, effectiveLimit);

        return new DashboardResponse(
                normalizePeriod(period),
                dateRange,
                marketSnapshot,
                topIncreases,
                topDecreases,
                categoryChanges,
                volatility);
    }

    private DashboardResponse.MarketSnapshot buildMarketSnapshot(LocalDate from, LocalDate to) {
        List<Price> currentPeriodPrices = priceRepository.findByIdDateBetween(from, to);
        BigDecimal currentAverage = calculateAverage(currentPeriodPrices);

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(periodDays - 1);

        List<Price> previousPeriodPrices = priceRepository.findByIdDateBetween(previousFrom, previousTo);
        BigDecimal previousAverage = calculateAverage(previousPeriodPrices);

        BigDecimal changeAbsolute = currentAverage.subtract(previousAverage);
        BigDecimal changePercentage =
                previousAverage.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : changeAbsolute
                                .multiply(BigDecimal.valueOf(100))
                                .divide(previousAverage, 2, RoundingMode.HALF_UP);

        return new DashboardResponse.MarketSnapshot(
                currentAverage, previousAverage, changePercentage, changeAbsolute);
    }

    private List<DashboardResponse.CategoryChange> buildCategoryChanges(
            List<ProductChangeMetric> productChanges, int limit) {
        Map<String, List<ProductChangeMetric>> byCategory =
                productChanges.stream().collect(Collectors.groupingBy(ProductChangeMetric::category));

        return byCategory.entrySet().stream()
                .map(
                        entry -> {
                            List<ProductChangeMetric> categoryItems = entry.getValue();
                            BigDecimal avgChangePercentage =
                                    categoryItems.stream()
                                            .map(ProductChangeMetric::changePercentage)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .divide(
                                                    BigDecimal.valueOf(categoryItems.size()),
                                                    2,
                                                    RoundingMode.HALF_UP);

                            BigDecimal avgChangeAbsolute =
                                    categoryItems.stream()
                                            .map(ProductChangeMetric::changeAbsolute)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .divide(
                                                    BigDecimal.valueOf(categoryItems.size()),
                                                    2,
                                                    RoundingMode.HALF_UP);

                            return new DashboardResponse.CategoryChange(
                                    entry.getKey(),
                                    avgChangePercentage,
                                    avgChangeAbsolute,
                                    categoryItems.size());
                        })
                .sorted(
                        (a, b) ->
                                b.avgChangePercentage()
                                        .abs()
                                        .compareTo(a.avgChangePercentage().abs()))
                .limit(limit)
                .toList();
    }

    private DashboardResponse.VolatilitySummary buildVolatilitySummary(
            LocalDate from, LocalDate to, int limit) {
        List<Product> products = productRepository.findAll();
        List<DashboardResponse.VolatilityItem> items = new ArrayList<>();

        for (Product product : products) {
            List<Price> prices =
                    priceRepository.findByIdProductIdAndIdDateBetween(
                            product.getProductId(), from, to);
            if (prices.size() < 2) {
                continue;
            }

            double mean =
                    prices.stream()
                            .mapToDouble(price -> price.getPriceAverage().doubleValue())
                            .average()
                            .orElse(0.0);
            if (mean == 0.0) {
                continue;
            }

            double variance =
                    prices.stream()
                            .mapToDouble(price -> Math.pow(price.getPriceAverage().doubleValue() - mean, 2))
                            .average()
                            .orElse(0.0);
            double stdDev = Math.sqrt(variance);
            BigDecimal coefficientOfVariation =
                    BigDecimal.valueOf((stdDev / mean) * 100).setScale(2, RoundingMode.HALF_UP);

            items.add(
                    new DashboardResponse.VolatilityItem(
                            product.getProductId(),
                            product.getName(),
                            product.getCategory() != null
                                    ? product.getCategory().getName()
                                    : "Unknown",
                            coefficientOfVariation));
        }

        List<DashboardResponse.VolatilityItem> mostVolatile =
                items.stream()
                        .sorted(
                                (a, b) ->
                                        b.coefficientOfVariation()
                                                .compareTo(a.coefficientOfVariation()))
                        .limit(limit)
                        .toList();

        return new DashboardResponse.VolatilitySummary(mostVolatile);
    }

    private List<ProductChangeMetric> buildProductChanges(
            LocalDate from, LocalDate to, Integer categoryId) {
        List<Product> products;
        if (categoryId != null) {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return List.of();
            }
            products = productRepository.findByCategory(categoryOpt.get());
        } else {
            products = productRepository.findAll();
        }

        List<ProductChangeMetric> changes = new ArrayList<>();

        for (Product product : products) {
            List<Price> prices =
                    priceRepository.findByIdProductIdAndIdDateBetween(
                            product.getProductId(), from, to);

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
                    priceBefore.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : changeAbsolute
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(priceBefore, 2, RoundingMode.HALF_UP);

            String changeDirection =
                    changePercentage.compareTo(BigDecimal.ZERO) > 0
                            ? "increase"
                            : changePercentage.compareTo(BigDecimal.ZERO) < 0
                                    ? "decrease"
                                    : "stable";

            changes.add(
                    new ProductChangeMetric(
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

        return changes;
    }

    private DashboardResponse.ProductChange toDashboardProductChange(ProductChangeMetric change) {
        return new DashboardResponse.ProductChange(
                change.productId(),
                change.productName(),
                change.category(),
                change.priceBefore(),
                change.priceAfter(),
                change.changePercentage(),
                change.changeAbsolute(),
                change.changeDirection());
    }

    private BigDecimal calculateAverage(List<Price> prices) {
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return prices.stream()
                .map(Price::getPriceAverage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
    }

    private DateRange resolveDateRangeByPeriod(String period, LocalDate effectiveTo) {
        String normalizedPeriod = normalizePeriod(period);
        LocalDate effectiveFrom;
        switch (normalizedPeriod) {
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
        return new DateRange(effectiveFrom, effectiveTo);
    }

    private DateRange resolveEffectiveDateRange(LocalDate from, LocalDate to, int defaultDays) {
        LocalDate latestAvailableDate = resolveLatestAvailableDate();
        if (from != null && from.isAfter(latestAvailableDate)) {
            throw new IllegalArgumentException(
                    "Requested date range is after the latest available data date: "
                            + latestAvailableDate);
        }

        LocalDate effectiveTo = to != null ? to : latestAvailableDate;

        if (effectiveTo.isAfter(latestAvailableDate)) {
            effectiveTo = latestAvailableDate;
        }

        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(defaultDays);
        if (effectiveFrom.isAfter(effectiveTo)) {
            effectiveFrom = effectiveTo.minusDays(defaultDays);
        }

        return new DateRange(effectiveFrom, effectiveTo);
    }

    private LocalDate resolveLatestAvailableDate() {
        LocalDate latestDate = priceRepository.findLatestDate();
        return latestDate != null ? latestDate : LocalDate.now();
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return "30d";
        }
        String normalized = period.toLowerCase();
        if ("7d".equals(normalized)
                || "30d".equals(normalized)
                || "90d".equals(normalized)
                || "1y".equals(normalized)) {
            return normalized;
        }
        return "30d";
    }

    private record ProductChangeMetric(
            Integer productId,
            String productName,
            String category,
            BigDecimal priceBefore,
            BigDecimal priceAfter,
            BigDecimal changePercentage,
            BigDecimal changeAbsolute,
            String changeDirection) {}
}
