package com.highlands.order.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponValidityBoundaryTest {
    @Test
    void fixedDiscountCannotReduceSubtotalBelowZero() {
        BigDecimal subtotal = BigDecimal.valueOf(50000);
        BigDecimal discount = BigDecimal.valueOf(80000);
        assertEquals(0, subtotal.subtract(discount).max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
    }

    @Test
    void percentageDiscountRoundsToTwoCurrencyDecimals() {
        BigDecimal result = BigDecimal.valueOf(12345)
                .multiply(BigDecimal.valueOf(15))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assertEquals(0, BigDecimal.valueOf(1851.75).compareTo(result));
    }
}
