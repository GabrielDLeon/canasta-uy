package uy.eleven.canasta.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.CategoryRepository;
import uy.eleven.canasta.repository.PriceRepository;
import uy.eleven.canasta.repository.ProductRepository;
import uy.eleven.canasta.testsupport.IntegrationContainers;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class PriceRepositoryIT extends IntegrationContainers {

    @Autowired private PriceRepository priceRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        priceRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Arroz");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setProductId(15);
        product.setName("Arroz Blanco 1kg");
        product.setBrand("Brand");
        product.setSpecification("1kg");
        product.setCategory(category);
        productRepository.save(product);

        Price p1 =
                TestDataFactory.price(
                        15,
                        LocalDate.of(2025, 1, 1),
                        new BigDecimal("90.00"),
                        new BigDecimal("110.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"));
        Price p2 =
                TestDataFactory.price(
                        15,
                        LocalDate.of(2025, 1, 2),
                        new BigDecimal("100.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("110.00"),
                        new BigDecimal("110.00"));
        priceRepository.saveAll(List.of(p1, p2));
    }

    @Test
    void findsPricesByDateRange() {
        List<Price> prices =
                priceRepository.findByIdProductIdAndIdDateBetween(
                        15, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertEquals(2, prices.size());
    }

    @Test
    void calculatesAggregatesForDateRange() {
        BigDecimal avg =
                priceRepository.calculateAveragePriceForProduct(
                        15, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        BigDecimal min =
                priceRepository.calculateMinPriceForProduct(
                        15, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        BigDecimal max =
                priceRepository.calculateMaxPriceForProduct(
                        15, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertEquals(0, new BigDecimal("105.0").compareTo(avg));
        assertEquals(0, new BigDecimal("100.00").compareTo(min));
        assertEquals(0, new BigDecimal("110.00").compareTo(max));
    }
}
