package com.highlands.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth-rate-limit")
public record AuthRateLimitProperties(int requests, long windowSeconds) { }
