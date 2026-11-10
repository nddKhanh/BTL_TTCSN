package com.highlands.order.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorCodeTest {
    @Test
    void exposesStableCodesForCoreDomains() {
        assertEquals("AUTHENTICATION_FAILED", ApiErrorCode.AUTHENTICATION_FAILED.name());
        assertEquals("COUPON_INVALID", ApiErrorCode.COUPON_INVALID.name());
        assertEquals("ORDER_LOOKUP_PROOF_REQUIRED", ApiErrorCode.ORDER_LOOKUP_PROOF_REQUIRED.name());
        assertEquals("WORKFLOW_CONFLICT", ApiErrorCode.WORKFLOW_CONFLICT.name());
        assertEquals("LEGACY_ENDPOINT_DISABLED", ApiErrorCode.LEGACY_ENDPOINT_DISABLED.name());
        assertTrue(ApiErrorCode.values().length >= 12);
    }
}
