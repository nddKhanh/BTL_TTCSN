package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ProductResponse;
import com.highlands.order.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Tra cứu sản phẩm đang bán")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Liệt kê sản phẩm", description = "Lọc theo categoryId hoặc keyword; trả về envelope ApiResponse.")
    public ApiResponse<List<ProductResponse>> getProducts(@RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.success(productService.getProducts(categoryId, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy sản phẩm theo ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy sản phẩm")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm với mã PRODUCT_NOT_FOUND")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}
