package com.highlands.order.application.fulfillment;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.fulfillment.*;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.dto.WorkflowHistoryResponse;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.FulfillmentHistory;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.FulfillmentHistoryRepository;
import com.highlands.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FulfillmentApplicationService {

    private final OrderRepository orders;
    private final FulfillmentHistoryRepository history;
    private final ApprovalPort approvalPort;

    public FulfillmentApplicationService(
            OrderRepository orders,
            FulfillmentHistoryRepository history,
            ApprovalPort approvalPort
    ) {
        this.orders = orders;
        this.history = history;
        this.approvalPort = approvalPort;
    }

    @Transactional
    public OrderResponse execute(
            Long orderId,
            FulfillmentAction action,
            ApprovalActor actor,
            String reason,
            String requestId,
            long expectedVersion
    ) {
        if (!actor.hasAuthority("ORDER_FULFILL")) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện đơn hàng");
        }
        Order order = findOrder(orderId);
        ApprovalPort.Snapshot approval = approvalPort.getByOrderId(orderId, actor);
        var previous = history.findByOrder_IdAndRequestId(orderId, requestId);
        if (previous.isPresent()) {
            if (!action.name().equals(previous.get().getAction())) {
                throw conflict("Request ID đã được dùng cho hành động khác");
            }
            return OrderResponse.from(order, approval, approvalPort.availableActionDetails(approval, actor), actor);
        }

        long currentVersion = order.getVersion() == null ? 0 : order.getVersion();
        if (currentVersion != expectedVersion) {
            throw conflict("Đơn hàng đã thay đổi; hãy tải lại dữ liệu");
        }
        FulfillmentStatus fromStatus = order.getFulfillmentStatus();
        if (!FulfillmentPolicy.availableActions(approval.status(), fromStatus, actor).contains(action)) {
            throw conflict("Hành động thực hiện đơn không hợp lệ ở trạng thái hiện tại");
        }
        if (action == FulfillmentAction.CANCEL && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Hủy thực hiện đơn phải có lý do");
        }

        FulfillmentStatus toStatus = FulfillmentPolicy.target(action);
        order.setFulfillmentStatus(toStatus);
        order.setStatus(legacyStatus(toStatus));
        order = orders.saveAndFlush(order);
        history.save(FulfillmentHistory.builder()
                .order(order)
                .action(action.name())
                .actorId(actor.actorId())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(normalizeReason(reason))
                .requestId(requestId)
                .build());
        return OrderResponse.from(order, approval, approvalPort.availableActionDetails(approval, actor), actor);
    }

    public List<WorkflowHistoryResponse> history(Long orderId, ApprovalActor actor) {
        Order order = findOrder(orderId);
        ApprovalPort.Snapshot approval = approvalPort.getByOrderId(orderId, actor);
        if (!actor.actorId().equals(approval.ownerActorId()) && !actor.hasAuthority("ORDER_READ")) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch sử thực hiện đơn");
        }
        return history.findByOrder_IdOrderByCreatedAtAscIdAsc(order.getId()).stream()
                .map(item -> new WorkflowHistoryResponse(
                        item.getAction(),
                        item.getActorId(),
                        item.getFromStatus().name(),
                        item.getToStatus().name(),
                        item.getReason(),
                        item.getCreatedAt(),
                        item.getRequestId()
                ))
                .toList();
    }

    private Order findOrder(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
    }

    private OrderStatus legacyStatus(FulfillmentStatus status) {
        return switch (status) {
            case NOT_STARTED, PREPARING -> OrderStatus.CONFIRMED;
            case DELIVERING -> OrderStatus.DELIVERING;
            case COMPLETED -> OrderStatus.COMPLETED;
            case CANCELLED -> OrderStatus.CANCELLED;
        };
    }

    private ApiDomainException conflict(String message) {
        return new ApiDomainException(ApiErrorCode.WORKFLOW_CONFLICT, HttpStatus.CONFLICT, message);
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
