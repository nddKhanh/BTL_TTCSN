package com.highlands.order.service;

import com.highlands.order.model.Category;
import com.highlands.order.dto.CategoryRequest;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.repository.CategoryRepository;
import com.highlands.order.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional public Category create(CategoryRequest request) { return categoryRepository.save(Category.builder().code(request.code().trim().toUpperCase()).name(request.name().trim()).imageUrl(request.imageUrl()).build()); }
    @Transactional public Category update(Long id, CategoryRequest request) { Category category = find(id); category.setCode(request.code().trim().toUpperCase()); category.setName(request.name().trim()); category.setImageUrl(request.imageUrl()); return category; }
    @Transactional public void delete(Long id) { if (productRepository.existsByCategoryId(id)) throw new IllegalArgumentException("Không thể xóa danh mục còn sản phẩm"); categoryRepository.delete(find(id)); }
    private Category find(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id)); }
}
