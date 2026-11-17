package com.highlands.order.controller;

import com.highlands.order.application.approval.WorkflowActorResolver;
import com.highlands.order.application.order.CancelOrderService;
import com.highlands.order.application.order.OrderQueryService;
import com.highlands.order.application.order.PlaceOrderService;
import com.highlands.order.dto.*;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final PlaceOrderService placeOrders;
    private final OrderQueryService orderQueries;
    private final CancelOrderService cancelOrders;
    private final WorkflowActorResolver actors;

    public OrderController(
            PlaceOrderService placeOrders,
            OrderQueryService orderQueries,
            CancelOrderService cancelOrders,
            WorkflowActorResolver actors
    ) {
        this.placeOrders = placeOrders;
        this.orderQueries = orderQueries;
        this.cancelOrders = cancelOrders;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt đơn và tạo yêu cầu xét duyệt local")
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success("Đặt hàng thành công!", placeOrders.place(request, actors.resolve(user)));
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Tra cứu đơn bằng quyền sở hữu hoặc số điện thoại")
    public ApiResponse<OrderResponse> getOrderByCode(
            @PathVariable String orderCode,
            @RequestParam(required = false) String phone,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(orderQueries.getByCode(orderCode, phone, actors.resolve(user)));
    }

    @GetMapping("/my-orders")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.success(orderQueries.getMyOrders(actors.resolve(user)));
    }

    @PatchMapping("/{orderId}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rút yêu cầu xét duyệt qua endpoint tương thích")
    public ApiResponse<OrderResponse> cancelMyOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(
                "Hủy đơn hàng thành công!",
                cancelOrders.withdraw(orderId, actors.resolve(user))
        );
    }

    @GetMapping("/admin/all")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(orderQueries.getAllOrders(
                status,
                keyword,
                createdFrom,
                createdTo,
                actors.resolve(user)
        ));
    }

    @GetMapping("/admin/page")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Page<OrderResponse>> getOrdersPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(orderQueries.getAllOrdersPage(
                status,
                keyword,
                createdFrom,
                createdTo,
                page,
                size,
                actors.resolve(user)
        ));
    }

    @Deprecated
    @PatchMapping("/admin/{orderId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Endpoint đổi trạng thái cũ đã bị khóa", deprecated = true)
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        throw new ApiDomainException(
                ApiErrorCode.LEGACY_ENDPOINT_DISABLED,
                HttpStatus.GONE,
                "Hãy dùng approval hoặc fulfillment action API"
        );
    }
}
