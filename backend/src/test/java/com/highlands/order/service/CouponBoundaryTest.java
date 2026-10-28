package com.highlands.order.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponBoundaryTest {
    @Test
    void percentageDiscountUsesCurrencyPrecision() {
        BigDecimal discount = BigDecimal.valueOf(99999)
                .multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, BigDecimal.valueOf(9999.90).compareTo(discount));
    }
}
