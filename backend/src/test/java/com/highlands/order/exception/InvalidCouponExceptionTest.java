package com.highlands.order.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidCouponExceptionTest {
    @Test
    void keepsBusinessMessage() {
        InvalidCouponException exception = new InvalidCouponException("Coupon expired");
        assertEquals("Coupon expired", exception.getMessage());
    }
}
