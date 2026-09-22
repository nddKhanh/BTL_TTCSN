package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.model.Topping;
import com.highlands.order.service.ToppingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/toppings")
public class ToppingController {

    private final ToppingService toppingService;

    public ToppingController(ToppingService toppingService) {
        this.toppingService = toppingService;
    }

    @GetMapping
    public ApiResponse<List<Topping>> getAllToppings() {
        return ApiResponse.success(toppingService.getAllToppings());
    }
}
