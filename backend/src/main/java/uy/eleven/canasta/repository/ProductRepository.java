package uy.eleven.canasta.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategory(Category category);

    Page<Product> findByCategory(Category category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

    List<Product> findByBrand(String brand);

    List<Product> findByNameContainingIgnoreCase(String name);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> searchByName(@Param("name") String name);

    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId")
    Page<Product> findByCategory(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds")
    List<Product> findByProductIds(@Param("productIds") List<Integer> productIds);

    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findProductById(@Param("id") Integer id);
}
