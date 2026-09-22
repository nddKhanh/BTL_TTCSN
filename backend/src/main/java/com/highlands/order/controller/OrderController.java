package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.UpdateOrderStatusRequest;
import com.highlands.order.model.Order;
import com.highlands.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order newOrder = orderService.createOrder(request);
        return ApiResponse.success("Đặt hàng thành công!", newOrder);
    }

    @GetMapping("/{orderCode}")
    public ApiResponse<Order> getOrderByCode(@PathVariable String orderCode) {
        return ApiResponse.success(orderService.getOrderByCode(orderCode));
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<Order>> getAllOrders() {
        return ApiResponse.success(orderService.getAllOrders());
    }

    @PatchMapping("/admin/{orderId}/status")
    public ApiResponse<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order updated = orderService.updateOrderStatus(orderId, request.status());
        return ApiResponse.success("Cập nhật trạng thái đơn thành công!", updated);
    }
}
