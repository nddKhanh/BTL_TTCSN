package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ToppingRequest;
import com.highlands.order.model.Topping;
import com.highlands.order.service.ToppingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/toppings")
public class AdminToppingController {

    private final ToppingService service;

    public AdminToppingController(ToppingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Topping> create(@Valid @RequestBody ToppingRequest request) {
        return ApiResponse.success("Tạo topping thành công", service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Topping> update(@PathVariable Long id, @Valid @RequestBody ToppingRequest request) {
        return ApiResponse.success("Cập nhật topping thành công", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
