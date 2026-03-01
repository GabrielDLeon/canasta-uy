package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.eleven.canasta.dto.category.CategoryProductsResponse;
import uy.eleven.canasta.dto.category.CategoryStatsResponse;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.CategoryRepository;
import uy.eleven.canasta.repository.ProductRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PriceService priceService;
    @InjectMocks private CategoryService categoryService;

    @Test
    void getCategoryProductsResponseReturnsNullWhenCategoryMissing() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());
        CategoryProductsResponse response =
                categoryService.getCategoryProductsResponse(
                        1, org.springframework.data.domain.PageRequest.of(0, 20));

        assertNull(response);
    }

    @Test
    void calculateCategoryStatsReturnsZeroesWhenNoPrices() {
        Category category = TestDataFactory.category(1, "Arroz");
        Product product = TestDataFactory.product(1, "Arroz A", category);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.findByCategory(category)).thenReturn(List.of(product));
        when(priceService.getPricesByProductAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        CategoryStatsResponse response =
                categoryService.calculateCategoryStats(
                        1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1));

        assertEquals(0, response.stats().avgPrice().compareTo(java.math.BigDecimal.ZERO));
        assertEquals(1, response.productCount());
    }
}
