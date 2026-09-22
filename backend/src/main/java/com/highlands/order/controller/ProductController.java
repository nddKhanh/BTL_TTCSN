package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.model.Product;
import com.highlands.order.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<Product>> getProducts(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(productService.getProducts(categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}
