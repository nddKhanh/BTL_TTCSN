package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.model.Category;
import com.highlands.order.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;
    public AdminCategoryController(CategoryService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Category> create(@Valid @RequestBody CategoryRequest request) { return ApiResponse.success("Tạo danh mục thành công", service.create(request)); }
    @PutMapping("/{id}") public ApiResponse<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) { return ApiResponse.success("Cập nhật danh mục thành công", service.update(id, request)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
