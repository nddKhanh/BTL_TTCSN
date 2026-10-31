package com.highlands.order.service;

import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Product;
import com.highlands.order.model.Category;
import com.highlands.order.dto.ProductRequest;
import com.highlands.order.dto.ProductResponse;
import com.highlands.order.repository.CategoryRepository;
import com.highlands.order.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponse> getProducts(Long categoryId, String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        List<Product> products;
        if (categoryId != null) {
            products = query.isEmpty()
                    ? productRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                    : productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndIsActiveTrue(categoryId, query);
        } else {
            products = query.isEmpty()
                    ? productRepository.findByIsActiveTrue()
                    : productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query);
        }
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Page<ProductResponse> getProductsPage(Long categoryId, String keyword, Pageable pageable) {
        String query = keyword == null ? "" : keyword.trim();
        Page<Product> products;
        if (categoryId != null) {
            products = query.isEmpty()
                    ? productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                    : productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndIsActiveTrue(categoryId, query, pageable);
        } else {
            products = query.isEmpty()
                    ? productRepository.findByIsActiveTrue(pageable)
                    : productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query, pageable);
        }
        return products.map(ProductResponse::from);
    }

    public List<ProductResponse> getAllProductsAdmin() {
        return productRepository.findAllByOrderByIdDesc().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .category(category(request.categoryId()))
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .imageUrl(request.imageUrl())
                .isActive(request.isActive())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
        product.setCategory(category(request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setImageUrl(request.imageUrl());
        product.setIsActive(request.isActive());
        return ProductResponse.from(product);
    }

    @Transactional
    public void hide(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
        product.setIsActive(false);
    }

    private Category category(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id));
    }
}
