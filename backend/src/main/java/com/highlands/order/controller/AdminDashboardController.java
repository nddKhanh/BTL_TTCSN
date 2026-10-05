package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.DashboardStatsResponse;
import com.highlands.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final OrderService orderService;

    public AdminDashboardController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        return ApiResponse.success(orderService.getDashboardStats());
    }
}
