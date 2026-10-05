package com.highlands.order.dto;

import java.math.BigDecimal;

public record ValidateCouponResponse(
    String code,
    BigDecimal discountAmount,
    String message
) {}
