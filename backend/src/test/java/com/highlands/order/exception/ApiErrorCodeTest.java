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
        assertTrue(ApiErrorCode.values().length >= 12);
    }
}
