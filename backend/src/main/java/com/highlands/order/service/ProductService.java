package com.highlands.order.service;

import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Product;
import com.highlands.order.model.Category;
import com.highlands.order.dto.ProductRequest;
import com.highlands.order.repository.CategoryRepository;
import com.highlands.order.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> getProducts(Long categoryId, String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (categoryId != null) {
            if (!query.isEmpty()) return productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndIsActiveTrue(categoryId, query);
            return productRepository.findByCategoryIdAndIsActiveTrue(categoryId);
        }
        if (!query.isEmpty()) return productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query);
        return productRepository.findByIsActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
    }

    @Transactional public Product create(ProductRequest request) { return productRepository.save(Product.builder().category(category(request.categoryId())).name(request.name().trim()).description(request.description()).basePrice(request.basePrice()).imageUrl(request.imageUrl()).isActive(request.isActive()).build()); }
    @Transactional public Product update(Long id, ProductRequest request) { Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id)); product.setCategory(category(request.categoryId())); product.setName(request.name().trim()); product.setDescription(request.description()); product.setBasePrice(request.basePrice()); product.setImageUrl(request.imageUrl()); product.setIsActive(request.isActive()); return product; }
    @Transactional public void hide(Long id) { Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id)); product.setIsActive(false); }
    private Category category(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id)); }
}
