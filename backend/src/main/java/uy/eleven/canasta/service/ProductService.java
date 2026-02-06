package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    public List<Product> searchProductsByName(String query) {
        return productRepository.searchByName(query);
    }
}
