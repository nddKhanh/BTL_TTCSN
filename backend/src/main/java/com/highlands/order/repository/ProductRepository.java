package com.highlands.order.repository;

import com.highlands.order.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @EntityGraph(attributePaths = {"sizes", "category"})
    List<Product> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"sizes", "category"})
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    @EntityGraph(attributePaths = {"sizes", "category"})
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword);

    @EntityGraph(attributePaths = {"sizes", "category"})
    List<Product> findByCategoryIdAndNameContainingIgnoreCaseAndIsActiveTrue(Long categoryId, String keyword);

    @EntityGraph(attributePaths = {"sizes", "category"})
    Optional<Product> findByIdAndIsActiveTrue(Long id);

    boolean existsByCategoryId(Long categoryId);
}
