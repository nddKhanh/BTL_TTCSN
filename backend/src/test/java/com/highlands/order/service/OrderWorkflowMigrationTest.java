package com.highlands.order.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OrderWorkflowMigrationTest {

    @Test
    void migrationCreatesVersionedIdempotentWorkflowAndBackfillsEveryOrder() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream("/db/migration/V8__create_order_workflow.sql")) {
            assertNotNull(stream);
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("ADD COLUMN version BIGINT NOT NULL DEFAULT 0"));
        assertTrue(migration.contains("CONSTRAINT uk_order_approvals_order UNIQUE (order_id)"));
        assertTrue(migration.contains("CONSTRAINT uk_approval_history_request UNIQUE (approval_id, request_id)"));
        assertTrue(migration.contains("CONSTRAINT uk_fulfillment_history_request UNIQUE (order_id, request_id)"));
        assertTrue(migration.contains("INSERT INTO order_approvals"));
        assertTrue(migration.contains("WHEN 'CANCELLED' THEN 'WITHDRAWN'"));
        assertTrue(migration.contains("('ADMIN', 'ORDER_APPROVE')"));
        assertTrue(migration.contains("('ADMIN', 'ORDER_FULFILL')"));
    }

    @Test
    void integrationMigrationAllowsExternalRecordAndActionNames() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V10__expand_approval_mapping_fields.sql")) {
            assertNotNull(stream);
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("approval_ref VARCHAR(100)"));
        assertTrue(migration.contains("action VARCHAR(64)"));
    }
}
