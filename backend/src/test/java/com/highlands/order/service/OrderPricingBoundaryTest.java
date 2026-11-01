package com.highlands.order.service;

import com.highlands.order.config.OrderPricingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderPricingBoundaryTest {
    @Test
    void freeShippingAppliesAtThreshold() {
        OrderPricingProperties pricing = new OrderPricingProperties(BigDecimal.valueOf(15000), BigDecimal.valueOf(200000));
        assertEquals(0, BigDecimal.valueOf(200000).compareTo(pricing.freeShippingThreshold()));
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(pricing.shippingFee()));
    }
}
