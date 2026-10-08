package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.UpdateOrderStatusRequest;
import com.highlands.order.model.Order;
import com.highlands.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/my-orders")
    public ApiResponse<List<Order>> getMyOrders(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem lịch sử đơn hàng");
        }
        return ApiResponse.success(orderService.getMyOrders(user.getUsername()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<Order> cancelMyOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để hủy đơn hàng");
        }
        Order cancelledOrder = orderService.cancelMyOrder(orderId, user.getUsername());
        return ApiResponse.success("Hủy đơn hàng thành công!", cancelledOrder);
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<Order>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo
    ) {
        return ApiResponse.success(orderService.getAllOrders(status, keyword, createdFrom, createdTo));
    }

    @PatchMapping("/admin/{orderId}/status")
    public ApiResponse<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order updated = orderService.updateOrderStatus(orderId, request.status());
        return ApiResponse.success("Cập nhật trạng thái đơn thành công!", updated);
    }
}
