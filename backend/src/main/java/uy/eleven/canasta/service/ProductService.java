package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import uy.eleven.canasta.exception.ProductNotFoundException;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.ProductRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<Product> getAllProductsPaginated(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size));
    }

    public Product getProductById(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> searchProductsByName(String query) {
        return productRepository.searchByName(query);
    }

    public Page<Product> searchProductsByNamePaginated(String query, int page, int size) {
        return productRepository.findByNameContainingIgnoreCase(query, PageRequest.of(page, size));
    }
}
