package com.highlands.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account-security")
public record AccountSecurityProperties(int maxFailures, long lockMinutes) { }
