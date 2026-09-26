package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.model.Product;
import com.highlands.order.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Product> create(
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.success("Tạo sản phẩm thành công", service.create(request));
    }
    @PutMapping("/{id}") public ApiResponse<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return ApiResponse.success("Cập nhật sản phẩm thành công", service.update(id, request)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void hide(@PathVariable Long id) { service.hide(id); }
}
