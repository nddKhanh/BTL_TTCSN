package com.highlands.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.approval")
public record ApprovalProviderProperties(String provider) {

    public ApprovalProviderProperties {
        provider = provider == null || provider.isBlank() ? "local" : provider.trim();
    }

    public boolean usesDxos() {
        return "dxos".equalsIgnoreCase(provider);
    }
}
