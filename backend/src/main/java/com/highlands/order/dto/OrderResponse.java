package com.highlands.order.dto;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.domain.fulfillment.FulfillmentAction;
import com.highlands.order.domain.fulfillment.FulfillmentPolicy;
import com.highlands.order.domain.fulfillment.FulfillmentStatus;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        String note,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        OrderStatus status,
        ApprovalStatus approvalStatus,
        String approvalWorkflowState,
        FulfillmentStatus fulfillmentStatus,
        Long version,
        Long approvalVersion,
        List<String> availableApprovalActions,
        List<WorkflowActionMetadata> availableApprovalActionMetadata,
        List<String> availableFulfillmentActions,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return from(order, null, ApprovalActor.anonymous());
    }

    public static OrderResponse from(
            Order order,
            ApprovalPort.Snapshot approval,
            ApprovalActor actor
    ) {
        return from(
                order,
                approval,
                approval == null
                        ? List.of()
                        : com.highlands.order.domain.approval.ApprovalPolicy
                                .availableActions(approval.status(), approval.ownerActorId(), actor).stream()
                                .map(action -> new ApprovalPort.AvailableAction(
                                        action.apiName(),
                                        action.apiName(),
                                        action.apiName().equals("reject")
                                ))
                                .toList(),
                actor
        );
    }

    public static OrderResponse from(
            Order order,
            ApprovalPort.Snapshot approval,
            List<ApprovalPort.AvailableAction> actions,
            ApprovalActor actor
    ) {
        List<String> approvalActions = actions.stream().map(ApprovalPort.AvailableAction::name).toList();
        List<WorkflowActionMetadata> approvalActionMetadata = actions.stream()
                .map(WorkflowActionMetadata::from)
                .toList();
        FulfillmentStatus fulfillmentStatus = order.getFulfillmentStatus();
        List<String> fulfillmentActions = approval == null || fulfillmentStatus == null
                ? List.of()
                : FulfillmentPolicy.availableActions(approval.status(), fulfillmentStatus, actor).stream()
                        .map(FulfillmentAction::apiName)
                        .sorted()
                        .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getNote(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getStatus(),
                approval == null ? null : approval.status(),
                approval == null ? null : approval.workflowState(),
                fulfillmentStatus,
                order.getVersion(),
                approval == null ? null : approval.version(),
                approvalActions,
                approvalActionMetadata,
                fulfillmentActions,
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
