package com.highlands.order.security;

public final class SecurityRoutePolicy {
    public static final String[] PUBLIC_ROUTES = {
            "/api/v1/auth/**", "/api/v1/categories/**", "/api/v1/products/**",
            "/api/v1/toppings/**", "/api/v1/coupons/**", "/api/v1/orders",
            "/api/v1/orders/*", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };
    public static final String[] ADMIN_ROUTES = {"/api/v1/orders/admin/**", "/api/v1/admin/**"};

    private SecurityRoutePolicy() { }
}
