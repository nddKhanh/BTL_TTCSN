package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service; }
    @GetMapping
    public ApiResponse<java.util.List<ProductResponse>> getAll() {
        return ApiResponse.success(service.getAllProductsAdmin());
    }

    @GetMapping("/page")
    public ApiResponse<Page<ProductResponse>> getPage(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Phân trang không hợp lệ");
        return ApiResponse.success(service.getAdminProductsPage(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.success("Tạo sản phẩm thành công", service.create(request));
    }
    @PutMapping("/{id}") public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return ApiResponse.success("Cập nhật sản phẩm thành công", service.update(id, request)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void hide(@PathVariable Long id) { service.hide(id); }
}
