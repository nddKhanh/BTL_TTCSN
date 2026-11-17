package com.highlands.order.application.approval;

import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.domain.fulfillment.FulfillmentStatus;
import com.highlands.order.dto.*;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ApprovalApplicationService {

    private final ApprovalPort approvalPort;
    private final OrderRepository orders;

    public ApprovalApplicationService(ApprovalPort approvalPort, OrderRepository orders) {
        this.approvalPort = approvalPort;
        this.orders = orders;
    }

    public ApprovalResponse get(Long orderId, ApprovalActor actor) {
        ApprovalPort.Snapshot snapshot = approvalPort.getByOrderId(orderId, actor);
        return ApprovalResponse.from(snapshot, approvalPort.availableActionDetails(snapshot, actor));
    }

    public List<WorkflowHistoryResponse> history(Long orderId, ApprovalActor actor) {
        ApprovalPort.Snapshot snapshot = approvalPort.getByOrderId(orderId, actor);
        return approvalPort.history(snapshot.approvalRef(), actor).stream()
                .map(item -> new WorkflowHistoryResponse(
                        item.action(),
                        item.actorId(),
                        item.fromState(),
                        item.toState(),
                        item.reason(),
                        item.createdAt(),
                        item.requestId()
                ))
                .toList();
    }

    @Transactional
    public OrderResponse execute(
            Long orderId,
            String action,
            ApprovalActor actor,
            String reason,
            String requestId,
            long expectedVersion
    ) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        ApprovalPort.Snapshot current = approvalPort.getByOrderId(orderId, actor);
        ApprovalPort.Snapshot result = approvalPort.execute(
                current.approvalRef(),
                action,
                actor,
                reason,
                requestId,
                expectedVersion
        );
        if (applyFinalResult(order, result.status())) {
            order = orders.saveAndFlush(order);
        }
        return OrderResponse.from(order, result, approvalPort.availableActionDetails(result, actor), actor);
    }

    @Transactional
    public OrderResponse legacyWithdraw(Long orderId, ApprovalActor actor) {
        ApprovalPort.Snapshot current = approvalPort.getByOrderId(orderId, actor);
        return execute(
                orderId,
                "withdraw",
                actor,
                "Rút yêu cầu từ endpoint tương thích",
                "legacy-withdraw:" + orderId + ":" + actor.actorId(),
                current.version()
        );
    }

    private boolean applyFinalResult(Order order, ApprovalStatus status) {
        if (status == ApprovalStatus.ACCEPTED && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            return true;
        }
        if ((status == ApprovalStatus.REJECTED || status == ApprovalStatus.WITHDRAWN)
                && order.getStatus() != OrderStatus.CANCELLED) {
            order.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
            order.setStatus(OrderStatus.CANCELLED);
            return true;
        }
        return false;
    }
}
