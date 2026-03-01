package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.dto.price.PriceListResponse;
import uy.eleven.canasta.dto.price.PriceSearchResponse;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.PriceRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock private PriceRepository priceRepository;
    @Mock private ProductService productService;
    @InjectMocks private PriceService priceService;

    @Test
    void validateDateRangeRejectsInvalidOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        priceService.validateDateRange(
                                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 1)));
    }

    @Test
    void validateDateRangeRejectsMoreThanOneYear() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        priceService.validateDateRange(
                                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 2)));
    }

    @Test
    void getProductPriceListResponseMapsRepositoryResults() {
        Product product =
                TestDataFactory.product(15, "Arroz", TestDataFactory.category(1, "Arroz"));
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 3);
        List<Price> prices =
                List.of(
                        TestDataFactory.price(
                                15,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("100"),
                                new BigDecimal("120"),
                                new BigDecimal("110"),
                                new BigDecimal("110")),
                        TestDataFactory.price(
                                15,
                                LocalDate.of(2025, 1, 2),
                                new BigDecimal("101"),
                                new BigDecimal("121"),
                                new BigDecimal("111"),
                                new BigDecimal("111")));
        when(productService.getProductById(15)).thenReturn(product);
        when(priceRepository.findByIdProductIdAndIdDateBetween(15, from, to)).thenReturn(prices);

        PriceListResponse response =
                priceService.getProductPriceListResponse(15, from, to, "daily");

        assertEquals(15, response.productId());
        assertEquals("Arroz", response.productName());
        assertEquals(2, response.prices().size());
        assertEquals(new BigDecimal("110"), response.prices().get(0).priceAvg());
    }

    @Test
    void searchPricesResponsePaginatesFilteredData() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        List<Price> allPrices =
                List.of(
                        TestDataFactory.price(
                                1,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("10"),
                                new BigDecimal("12"),
                                new BigDecimal("11"),
                                new BigDecimal("11")),
                        TestDataFactory.price(
                                1,
                                LocalDate.of(2025, 1, 2),
                                new BigDecimal("11"),
                                new BigDecimal("13"),
                                new BigDecimal("12"),
                                new BigDecimal("12")),
                        TestDataFactory.price(
                                2,
                                LocalDate.of(2025, 1, 3),
                                new BigDecimal("20"),
                                new BigDecimal("22"),
                                new BigDecimal("21"),
                                new BigDecimal("21")));
        when(priceRepository.findByIdDateBetween(from, to)).thenReturn(allPrices);

        PriceSearchResponse response = priceService.searchPricesResponse("1", from, to, 0, 1);

        assertEquals(1, response.prices().size());
        assertEquals(2, response.pagination().totalElements());
        assertEquals(2, response.pagination().totalPages());
        assertEquals(true, response.pagination().hasNext());
    }

    @Test
    void searchPricesResponseThrowsWhenProductIdsContainInvalidToken() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        assertThrows(
                NumberFormatException.class,
                () -> priceService.searchPricesResponse("1,abc", from, to, 0, 20));
    }
}
