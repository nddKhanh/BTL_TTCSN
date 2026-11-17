package com.highlands.order.domain.fulfillment;

public enum FulfillmentStatus {
    NOT_STARTED,
    PREPARING,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
