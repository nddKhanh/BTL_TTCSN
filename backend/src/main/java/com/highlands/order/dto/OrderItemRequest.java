package com.highlands.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderItemRequest(
    @NotNull(message = "ProductID không được để trống")
    Long productId,

    String sizeName,

    List<@NotBlank(message = "Tên topping không được để trống")
            @Size(max = 100, message = "Tên topping không được vượt quá 100 ký tự") String> toppings,

    String iceLevel,

    String sugarLevel,

    String note,

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    Integer quantity
) {}
