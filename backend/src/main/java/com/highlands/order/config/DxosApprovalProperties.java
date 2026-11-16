package com.highlands.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.approval.dxos")
public record DxosApprovalProperties(
        String baseUrl,
        String model,
        String entity,
        String realm,
        Duration connectTimeout,
        Duration readTimeout,
        Identity requester,
        Identity approver
) {

    public DxosApprovalProperties {
        baseUrl = defaultText(baseUrl, "http://localhost:19080");
        model = defaultText(model, "ttcsn_order_approval");
        entity = defaultText(entity, "approval");
        realm = defaultText(realm, "dxos");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(8) : readTimeout;
        requester = requester == null ? new Identity("", "") : requester;
        approver = approver == null ? new Identity("", "") : approver;
    }

    public void requireConfigured() {
        requireIdentity("requester", requester);
        requireIdentity("approver", approver);
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalStateException("DX-OS connect timeout phải lớn hơn 0");
        }
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalStateException("DX-OS read timeout phải lớn hơn 0");
        }
    }

    public String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record Identity(String clientId, String clientSecret) {
        public Identity {
            clientId = clientId == null ? "" : clientId;
            clientSecret = clientSecret == null ? "" : clientSecret;
        }
    }

    private static void requireIdentity(String name, Identity identity) {
        if (identity.clientId().isBlank() || identity.clientSecret().isBlank()) {
            throw new IllegalStateException("Thiếu credential DX-OS cho identity " + name);
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
