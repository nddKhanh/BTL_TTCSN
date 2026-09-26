package com.highlands.order.model;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED).contains(target);
            case CONFIRMED -> EnumSet.of(DELIVERING, CANCELLED).contains(target);
            case DELIVERING -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
