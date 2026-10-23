package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.dto.UpdateOrderStatusRequest;
import com.highlands.order.service.OrderService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Tạo, tra cứu và quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo đơn hàng", description = "Tạo đơn public hoặc gắn với tài khoản nếu gửi JWT.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đặt hàng thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sản phẩm, size, topping hoặc coupon không hợp lệ")
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String authenticatedEmail = user == null ? null : user.getUsername();
        OrderResponse newOrder = orderService.createOrder(request, authenticatedEmail);
        return ApiResponse.success("Đặt hàng thành công!", newOrder);
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Tra cứu đơn hàng", description = "Tra cứu bằng mã đơn; mã lỗi ORDER_NOT_FOUND nếu không tồn tại.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy đơn hàng")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng")
    public ApiResponse<OrderResponse> getOrderByCode(@PathVariable String orderCode) {
        return ApiResponse.success(orderService.getOrderByCode(orderCode));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Xem lịch sử đơn hàng", description = "Yêu cầu JWT của customer.")
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách đơn hàng")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Thiếu hoặc sai JWT")
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem lịch sử đơn hàng");
        }
        return ApiResponse.success(orderService.getMyOrders(user.getUsername()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelMyOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để hủy đơn hàng");
        }
        OrderResponse cancelledOrder = orderService.cancelMyOrder(orderId, user.getUsername());
        return ApiResponse.success("Hủy đơn hàng thành công!", cancelledOrder);
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo
    ) {
        return ApiResponse.success(orderService.getAllOrders(status, keyword, createdFrom, createdTo));
    }

    @PatchMapping("/admin/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse updated = orderService.updateOrderStatus(orderId, request.status());
        return ApiResponse.success("Cập nhật trạng thái đơn thành công!", updated);
    }
}
