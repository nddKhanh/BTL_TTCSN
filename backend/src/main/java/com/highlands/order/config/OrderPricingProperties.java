package com.highlands.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.order-pricing")
public record OrderPricingProperties(BigDecimal shippingFee, BigDecimal freeShippingThreshold) { }
