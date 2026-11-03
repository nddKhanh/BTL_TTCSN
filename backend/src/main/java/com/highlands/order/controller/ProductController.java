package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ProductResponse;
import com.highlands.order.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(@RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.success(productService.getProducts(categoryId, keyword));
    }

    @GetMapping("/page")
    public ApiResponse<Page<ProductResponse>> getProductsPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page en phải >= 0 và size phải từ 1 đến 100");
        }
        String sortProperty = switch (sort.toLowerCase()) {
            case "name", "basePrice", "id" -> sort;
            default -> throw new IllegalArgumentException("Trường sắp xếp không hợp lệ");
        };
        Sort.Direction sortDirection = switch (direction.toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Hướng sắp xếp không hợp lệ");
        };
        return ApiResponse.success(productService.getProductsPage(categoryId, keyword,
                PageRequest.of(page, size, Sort.by(sortDirection, sortProperty))));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}
