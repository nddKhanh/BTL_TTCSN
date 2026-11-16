package com.highlands.order.domain.fulfillment;

import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.approval.ApprovalStatus;

import java.util.Set;

public final class FulfillmentPolicy {

    private FulfillmentPolicy() {
    }

    public static Set<FulfillmentAction> availableActions(
            ApprovalStatus approvalStatus,
            FulfillmentStatus fulfillmentStatus,
            ApprovalActor actor
    ) {
        if (approvalStatus != ApprovalStatus.ACCEPTED || !actor.hasAuthority("ORDER_FULFILL")) {
            return Set.of();
        }
        return switch (fulfillmentStatus) {
            case NOT_STARTED -> Set.of(FulfillmentAction.START_PREPARATION, FulfillmentAction.CANCEL);
            case PREPARING -> Set.of(FulfillmentAction.DISPATCH, FulfillmentAction.CANCEL);
            case DELIVERING -> Set.of(FulfillmentAction.COMPLETE);
            case COMPLETED, CANCELLED -> Set.of();
        };
    }

    public static FulfillmentStatus target(FulfillmentAction action) {
        return switch (action) {
            case START_PREPARATION -> FulfillmentStatus.PREPARING;
            case DISPATCH -> FulfillmentStatus.DELIVERING;
            case COMPLETE -> FulfillmentStatus.COMPLETED;
            case CANCEL -> FulfillmentStatus.CANCELLED;
        };
    }
}
