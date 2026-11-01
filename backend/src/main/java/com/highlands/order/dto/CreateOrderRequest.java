package com.highlands.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank(message = "Tên khách hàng không được để trống")
    String customerName,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(\\+84|0)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ")
    String customerPhone,

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @jakarta.validation.constraints.Size(min = 10, max = 255, message = "Địa chỉ giao hàng phải từ 10 đến 255 ký tự")
    String deliveryAddress,

    String note,

    String couponCode,

    @NotEmpty(message = "Giỏ hàng không được để trống")
    List<@Valid OrderItemRequest> items
) {}
