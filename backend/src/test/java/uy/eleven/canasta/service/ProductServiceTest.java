package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import uy.eleven.canasta.exception.ProductNotFoundException;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.ProductRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(999));
    }

    @Test
    void getAllProductsPaginatedDelegatesToRepository() {
        Product product = TestDataFactory.product(1, "Arroz", TestDataFactory.category(1, "cat"));
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        Page<Product> result = productService.getAllProductsPaginated(0, 20);

        assertEquals(1, result.getContent().size());
    }
}
