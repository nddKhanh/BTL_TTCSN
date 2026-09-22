package com.highlands.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank(message = "Tên khách hàng không được để trống")
    String customerName,

    @NotBlank(message = "Số điện thoại không được để trống")
    String customerPhone,

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    String deliveryAddress,

    String note,

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    List<OrderItemRequest> items
) {}
