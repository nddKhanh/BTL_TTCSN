package com.highlands.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidateCouponRequest(
    @NotBlank(message = "Mã giảm giá không được để trống")
    String code,

    @NotNull(message = "Giá trị tạm tính không được để trống")
    @Min(value = 0, message = "Giá trị tạm tính không được âm")
    BigDecimal subtotal
) {}
