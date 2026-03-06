package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.dto.analytics.ComparisonResponse;
import uy.eleven.canasta.dto.analytics.DashboardResponse;
import uy.eleven.canasta.dto.analytics.InflationResponse;
import uy.eleven.canasta.dto.analytics.TopChangesResponse;
import uy.eleven.canasta.dto.analytics.TrendResponse;
import uy.eleven.canasta.exception.CategoryNotFoundException;
import uy.eleven.canasta.exception.ProductNotFoundException;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.CategoryRepository;
import uy.eleven.canasta.repository.PriceRepository;
import uy.eleven.canasta.repository.ProductRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private PriceRepository priceRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PriceService priceService;
    @InjectMocks private AnalyticsService analyticsService;

    @Test
    void calculateTrendThrowsWhenProductMissing() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () ->
                        analyticsService.calculateTrend(
                                999, LocalDate.now().minusDays(1), LocalDate.now(), false));
    }

    @Test
    void calculateTrendReturnsStableSummaryWhenNoPrices() {
        Product product = TestDataFactory.product(1, "Arroz", TestDataFactory.category(1, "cat"));
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 10);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(priceRepository.findByIdProductIdAndIdDateBetween(1, from, to)).thenReturn(List.of());

        TrendResponse response = analyticsService.calculateTrend(1, from, to, false);

        assertEquals("stable", response.summary().trend());
        assertNull(response.data());
    }

    @Test
    void calculateTrendDetectsIncreasingAndIncludesData() {
        Product product = TestDataFactory.product(1, "Arroz", TestDataFactory.category(1, "cat"));
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 3);
        List<Price> prices =
                new ArrayList<>(
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.of(2025, 1, 1),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        1,
                                        LocalDate.of(2025, 1, 3),
                                        new BigDecimal("130"),
                                        new BigDecimal("130"),
                                        new BigDecimal("130"),
                                        new BigDecimal("130"))));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(priceRepository.findByIdProductIdAndIdDateBetween(1, from, to)).thenReturn(prices);

        TrendResponse response = analyticsService.calculateTrend(1, from, to, true);

        assertEquals("increasing", response.summary().trend());
        assertEquals("up", response.summary().trendDirection());
        assertEquals(2, response.data().size());
    }

    @Test
    void calculateInflationThrowsWhenCategoryMissing() {
        when(categoryRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(
                CategoryNotFoundException.class,
                () ->
                        analyticsService.calculateInflation(
                                10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1), false));
    }

    @Test
    void calculateInflationReturnsZeroWhenCategoryHasNoProducts() {
        Category category = TestDataFactory.category(2, "Bebidas");
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 2, 1);
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(productRepository.findByCategory(category)).thenReturn(List.of());

        InflationResponse response = analyticsService.calculateInflation(2, from, to, false);

        assertEquals(BigDecimal.ZERO, response.summary().totalInflationPercentage());
        assertEquals(0, response.summary().productsCount());
    }

    @Test
    void compareProductsReturnsEmptyWhenNoProductsHaveData() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        when(productRepository.findById(2)).thenReturn(Optional.empty());

        ComparisonResponse response = analyticsService.compareProducts(List.of(1, 2), from, to);

        assertEquals(0, response.products().size());
        assertNull(response.comparison());
    }

    @Test
    void compareProductsIncludesDailyDataWhenIncludeDataIsTrue() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 3);
        Category category = TestDataFactory.category(1, "Aceites");
        Product product = TestDataFactory.product(1, "Aceite A", category);
        List<Price> prices =
                new ArrayList<>(
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.of(2025, 1, 2),
                                        new BigDecimal("90"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        1,
                                        LocalDate.of(2025, 1, 1),
                                        new BigDecimal("80"),
                                        new BigDecimal("90"),
                                        new BigDecimal("90"),
                                        new BigDecimal("90")),
                                TestDataFactory.price(
                                        1,
                                        LocalDate.of(2025, 1, 3),
                                        new BigDecimal("95"),
                                        new BigDecimal("110"),
                                        new BigDecimal("110"),
                                        new BigDecimal("110"))));

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(priceRepository.findByIdProductIdAndIdDateBetween(1, from, to)).thenReturn(prices);

        ComparisonResponse response = analyticsService.compareProducts(List.of(1), from, to, true);

        assertEquals(1, response.products().size());
        assertNotNull(response.products().get(0).data());
        assertEquals(3, response.products().get(0).data().size());
        assertEquals(LocalDate.of(2025, 1, 1), response.products().get(0).data().get(0).date());
        assertEquals(new BigDecimal("90"), response.products().get(0).data().get(0).priceAvg());
    }

    @Test
    void getTopChangesAppliesTypeFilter() {
        Category category = TestDataFactory.category(1, "Arroz");
        Product product = TestDataFactory.product(1, "Arroz A", category);
        List<Price> prices =
                new ArrayList<>(
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(30),
                                        new BigDecimal("90"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(1),
                                        new BigDecimal("70"),
                                        new BigDecimal("80"),
                                        new BigDecimal("80"),
                                        new BigDecimal("80"))));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(priceRepository.findByIdProductIdAndIdDateBetween(
                        eq(1),
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(prices);

        TopChangesResponse response = analyticsService.getTopChanges("30d", "decrease", 10, null);

        assertEquals(1, response.changes().size());
        assertEquals("decrease", response.changes().get(0).changeDirection());
    }

    @Test
    void getDashboardSummaryReturnsSnapshotAndRankings() {
        Category category = TestDataFactory.category(1, "Arroz");
        Product productA = TestDataFactory.product(1, "Producto A", category);
        Product productB = TestDataFactory.product(2, "Producto B", category);

        List<Price> pricesA =
                new ArrayList<>(
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(30),
                                        new BigDecimal("95"),
                                        new BigDecimal("105"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(1),
                                        new BigDecimal("115"),
                                        new BigDecimal("125"),
                                        new BigDecimal("120"),
                                        new BigDecimal("120"))));
        List<Price> pricesB =
                new ArrayList<>(
                        List.of(
                                TestDataFactory.price(
                                        2,
                                        LocalDate.now().minusDays(30),
                                        new BigDecimal("95"),
                                        new BigDecimal("105"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        2,
                                        LocalDate.now().minusDays(1),
                                        new BigDecimal("75"),
                                        new BigDecimal("85"),
                                        new BigDecimal("80"),
                                        new BigDecimal("80"))));

        when(productRepository.findAll()).thenReturn(List.of(productA, productB));
        when(priceRepository.findByIdProductIdAndIdDateBetween(
                        eq(1),
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(pricesA);
        when(priceRepository.findByIdProductIdAndIdDateBetween(
                        eq(2),
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(pricesB);
        when(priceRepository.findByIdDateBetween(
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(1),
                                        new BigDecimal("125"),
                                        new BigDecimal("135"),
                                        new BigDecimal("130"),
                                        new BigDecimal("130")),
                                TestDataFactory.price(
                                        2,
                                        LocalDate.now().minusDays(1),
                                        new BigDecimal("85"),
                                        new BigDecimal("95"),
                                        new BigDecimal("90"),
                                        new BigDecimal("90"))),
                        List.of(
                                TestDataFactory.price(
                                        1,
                                        LocalDate.now().minusDays(60),
                                        new BigDecimal("95"),
                                        new BigDecimal("105"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        2,
                                        LocalDate.now().minusDays(60),
                                        new BigDecimal("95"),
                                        new BigDecimal("105"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"))));

        DashboardResponse response = analyticsService.getDashboardSummary("30d", 5);

        assertEquals("30d", response.period());
        assertEquals(new BigDecimal("110.00"), response.marketSnapshot().avgPriceCurrent());
        assertEquals(new BigDecimal("100.00"), response.marketSnapshot().avgPricePrevious());
        assertEquals(1, response.topIncreases().size());
        assertEquals(1, response.topDecreases().size());
        assertEquals("Producto A", response.topIncreases().get(0).productName());
        assertEquals("Producto B", response.topDecreases().get(0).productName());
        assertTrue(response.volatility().mostVolatile().size() > 0);
    }

    @Test
    void calculateTrendClampsFutureToDateToLatestAvailableDate() {
        Product product = TestDataFactory.product(1, "Arroz", TestDataFactory.category(1, "cat"));
        LocalDate latestAvailable = LocalDate.of(2025, 12, 31);
        LocalDate expectedFrom = LocalDate.of(2025, 12, 1);
        LocalDate expectedTo = LocalDate.of(2025, 12, 31);

        when(priceRepository.findLatestDate()).thenReturn(latestAvailable);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(priceRepository.findByIdProductIdAndIdDateBetween(1, expectedFrom, expectedTo))
                .thenReturn(
                        new ArrayList<>(
                                List.of(
                                TestDataFactory.price(
                                        1,
                                        expectedFrom,
                                        new BigDecimal("90"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100"),
                                        new BigDecimal("100")),
                                TestDataFactory.price(
                                        1,
                                        expectedTo,
                                        new BigDecimal("100"),
                                        new BigDecimal("110"),
                                        new BigDecimal("110"),
                                        new BigDecimal("110")))));

        TrendResponse response =
                analyticsService.calculateTrend(
                        1, expectedFrom, LocalDate.of(2026, 3, 5), false);

        assertEquals(expectedTo, response.period().to());
        assertEquals("increasing", response.summary().trend());
    }

    @Test
    void calculateTrendThrowsWhenRequestedRangeStartsAfterLatestAvailableDate() {
        Product product = TestDataFactory.product(1, "Arroz", TestDataFactory.category(1, "cat"));
        LocalDate latestAvailable = LocalDate.of(2025, 12, 31);

        when(priceRepository.findLatestDate()).thenReturn(latestAvailable);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                analyticsService.calculateTrend(
                                        1,
                                        LocalDate.of(2026, 1, 1),
                                        LocalDate.of(2026, 3, 1),
                                        false));

        assertTrue(ex.getMessage().contains("latest available data date"));
    }
}
