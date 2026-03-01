package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.dto.analytics.ComparisonResponse;
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
                        org.mockito.ArgumentMatchers.eq(1),
                        org.mockito.ArgumentMatchers.any(LocalDate.class),
                        org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(prices);

        TopChangesResponse response = analyticsService.getTopChanges("30d", "decrease", 10, null);

        assertEquals(1, response.changes().size());
        assertEquals("decrease", response.changes().get(0).changeDirection());
    }
}
