package com.highlands.order.security;

import com.highlands.order.dto.ProductRequest;
import com.highlands.order.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationMatrixTest {
    @Test
    void customerAndAdminHaveExpectedPermissionBoundary() throws IOException {
        String seed;
        try (var stream = getClass().getResourceAsStream("/db/migration/V6__create_role_permissions.sql")) {
            assertNotNull(stream);
            seed = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(seed.contains("('CUSTOMER', 'ORDER_READ_OWN')"));
        assertFalse(seed.contains("('CUSTOMER', 'CATALOG_WRITE')"));
        assertFalse(seed.contains("('CUSTOMER', 'REPORT_READ')"));
        assertTrue(seed.contains("('ADMIN', 'CATALOG_WRITE')"));
        assertTrue(seed.contains("('ADMIN', 'REPORT_READ')"));

        String workflowSeed;
        try (var stream = getClass().getResourceAsStream("/db/migration/V8__create_order_workflow.sql")) {
            assertNotNull(stream);
            workflowSeed = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(workflowSeed.contains("('ADMIN', 'ORDER_APPROVE')"));
        assertTrue(workflowSeed.contains("('ADMIN', 'ORDER_FULFILL')"));
        assertFalse(workflowSeed.contains("('CUSTOMER', 'ORDER_APPROVE')"));
    }

    @Test
    void catalogMutationRequiresExplicitPermission() throws NoSuchMethodException {
        PreAuthorize rule = ProductService.class.getMethod("create", ProductRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(rule);
        assertEquals("hasAuthority('CATALOG_WRITE')", rule.value());
    }
}
