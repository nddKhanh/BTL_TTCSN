package com.highlands.order.domain;

import com.highlands.order.domain.approval.*;
import com.highlands.order.domain.fulfillment.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowPolicyTest {

    @Test
    void ownerCanWithdrawButCannotApproveOwnOrder() {
        ApprovalActor ownerAdmin = new ApprovalActor(
                "user:1",
                1L,
                true,
                Set.of("ORDER_APPROVE", "ORDER_READ")
        );

        assertEquals(
                Set.of(ApprovalAction.WITHDRAW),
                ApprovalPolicy.availableActions(ApprovalStatus.PENDING, "user:1", ownerAdmin)
        );
    }

    @Test
    void approverCanDecidePendingRequestAndTerminalRequestIsImmutable() {
        ApprovalActor approver = new ApprovalActor(
                "user:2",
                2L,
                true,
                Set.of("ORDER_APPROVE", "ORDER_READ")
        );

        assertEquals(
                Set.of(ApprovalAction.APPROVE, ApprovalAction.REJECT),
                ApprovalPolicy.availableActions(ApprovalStatus.PENDING, "user:1", approver)
        );
        assertTrue(ApprovalPolicy.availableActions(ApprovalStatus.ACCEPTED, "user:1", approver).isEmpty());
    }

    @Test
    void fulfillmentActionsFollowAcceptedOrderState() {
        ApprovalActor staff = new ApprovalActor(
                "user:2",
                2L,
                true,
                Set.of("ORDER_FULFILL")
        );

        assertEquals(
                Set.of(FulfillmentAction.START_PREPARATION, FulfillmentAction.CANCEL),
                FulfillmentPolicy.availableActions(ApprovalStatus.ACCEPTED, FulfillmentStatus.NOT_STARTED, staff)
        );
        assertEquals(
                Set.of(FulfillmentAction.DISPATCH, FulfillmentAction.CANCEL),
                FulfillmentPolicy.availableActions(ApprovalStatus.ACCEPTED, FulfillmentStatus.PREPARING, staff)
        );
        assertEquals(
                Set.of(FulfillmentAction.COMPLETE),
                FulfillmentPolicy.availableActions(ApprovalStatus.ACCEPTED, FulfillmentStatus.DELIVERING, staff)
        );
        assertTrue(FulfillmentPolicy.availableActions(
                ApprovalStatus.PENDING,
                FulfillmentStatus.NOT_STARTED,
                staff
        ).isEmpty());
    }
}
