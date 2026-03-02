package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import uy.eleven.canasta.dto.category.CategoryProductsResponse;
import uy.eleven.canasta.dto.category.CategoryStatsResponse;
import uy.eleven.canasta.dto.common.DateRange;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.CategoryRepository;
import uy.eleven.canasta.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceService priceService;

    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Page<Category> getAllCategoriesPaginated(int page, int size) {
        return categoryRepository.findAll(PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    public Page<Category> searchCategoriesByNamePaginated(String query, int page, int size) {
        return categoryRepository.findByNameContainingIgnoreCase(
                query, PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    public Optional<Category> getCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    public Page<Product> getProductsByCategory(Integer categoryId, Pageable pageable) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return Page.empty();
        }
        return productRepository.findByCategory(categoryOpt.get(), pageable);
    }

    public CategoryProductsResponse getCategoryProductsResponse(
            Integer categoryId, Pageable pageable) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return null;
        }

        Category category = categoryOpt.get();
        Page<Product> productPage = productRepository.findByCategory(category, pageable);

        List<CategoryProductsResponse.ProductSummary> productSummaries =
                productPage.getContent().stream()
                        .map(
                                p ->
                                        new CategoryProductsResponse.ProductSummary(
                                                p.getProductId(),
                                                p.getName(),
                                                p.getBrand(),
                                                p.getSpecification()))
                        .toList();

        return new CategoryProductsResponse(
                categoryId,
                category.getName(),
                productSummaries,
                null // PaginationInfo se setea en el controller
                );
    }

    public CategoryStatsResponse calculateCategoryStats(
            Integer categoryId, LocalDate from, LocalDate to) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return null;
        }

        Category category = categoryOpt.get();
        List<Product> products = productRepository.findByCategory(category);

        List<Price> allPrices =
                products.stream()
                        .flatMap(
                                p ->
                                        priceService
                                                .getPricesByProductAndDateRange(
                                                        p.getProductId(), from, to)
                                                .stream())
                        .toList();

        if (allPrices.isEmpty()) {
            return new CategoryStatsResponse(
                    categoryId,
                    category.getName(),
                    products.size(),
                    new DateRange(from, to),
                    new CategoryStatsResponse.PriceStats(
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        BigDecimal avgPrice =
                allPrices.stream()
                        .map(Price::getPriceAverage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(allPrices.size()), 2, BigDecimal.ROUND_HALF_UP);

        BigDecimal minPrice =
                allPrices.stream()
                        .map(Price::getPriceMinimum)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice =
                allPrices.stream()
                        .map(Price::getPriceMaximum)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

        BigDecimal medianPrice =
                allPrices.stream()
                        .map(Price::getPriceMedian)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(allPrices.size()), 2, BigDecimal.ROUND_HALF_UP);

        return new CategoryStatsResponse(
                categoryId,
                category.getName(),
                products.size(),
                new DateRange(from, to),
                new CategoryStatsResponse.PriceStats(avgPrice, minPrice, maxPrice, medianPrice));
    }
}
