package com.highlands.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderItemRequest(
    @NotNull(message = "ProductID không được để trống")
    Long productId,

    String sizeName,

    List<String> toppings,

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    Integer quantity
) {}
