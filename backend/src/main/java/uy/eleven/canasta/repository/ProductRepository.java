package uy.eleven.canasta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Find products by category
    List<Product> findByCategory(Category category);

    // Find products by brand
    List<Product> findByBrand(String brand);

    // Find product by name (case-insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Find products with LIKE using @Query
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> searchByName(@Param("name") String name);
}
