package com.highlands.order.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityRoutePolicyTest {
    @Test
    void keepsPublicCustomerAndAdminRouteGroupsSeparate() {
        assertTrue(Arrays.asList(SecurityRoutePolicy.PUBLIC_ROUTES).contains("/api/v1/auth/**"));
        assertTrue(Arrays.asList(SecurityRoutePolicy.PUBLIC_ROUTES).contains("/api/v1/orders/*"));
        assertTrue(Arrays.asList(SecurityRoutePolicy.ADMIN_ROUTES).contains("/api/v1/admin/**"));
    }
}
