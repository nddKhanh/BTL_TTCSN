package com.highlands.order.service;

import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Product;
import com.highlands.order.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProducts(Long categoryId) {
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndIsActiveTrue(categoryId);
        }
        return productRepository.findByIsActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm có ID: " + id));
    }
}
