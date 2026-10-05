package com.highlands.order.dto;

import com.highlands.order.model.CouponType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
    @NotBlank(message = "Mã coupon không được để trống")
    String code,

    @NotNull(message = "Loại giảm giá không được để trống")
    CouponType discountType,

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Min(value = 0, message = "Giá trị giảm không được âm")
    BigDecimal discountValue,

    BigDecimal minOrderAmount,

    BigDecimal maxDiscountAmount,

    Boolean active,

    LocalDateTime expiryDate
) {}
