package com.highlands.order.application.approval;

import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.approval.ApprovalPolicy;
import com.highlands.order.domain.approval.ApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface ApprovalPort {

    Pattern ACTION_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

    record Submission(
            Long orderId,
            String orderCode,
            String customerName,
            BigDecimal totalAmount
    ) {
    }

    record Snapshot(
            String approvalRef,
            Long orderId,
            ApprovalStatus status,
            String workflowState,
            String ownerActorId,
            String decidedBy,
            LocalDateTime createdAt,
            LocalDateTime decidedAt,
            long version
    ) {
    }

    record HistoryEntry(
            String action,
            String actorId,
            String fromState,
            String toState,
            String reason,
            LocalDateTime createdAt,
            String requestId
    ) {
    }

    record AvailableAction(
            String name,
            String label,
            boolean reasonRequired
    ) {
        public AvailableAction {
            if (name == null || !ACTION_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Tên action không hợp lệ: " + name);
            }
            if (label == null || label.isBlank()) {
                label = name;
            }
        }
    }

    Snapshot submit(Submission submission, ApprovalActor actor, String requestId);

    Snapshot getByOrderId(Long orderId, ApprovalActor actor);

    Map<Long, Snapshot> findByOrderIds(Collection<Long> orderIds);

    Snapshot execute(
            String approvalRef,
            String action,
            ApprovalActor actor,
            String reason,
            String requestId,
            long expectedVersion
    );

    List<HistoryEntry> history(String approvalRef, ApprovalActor actor);

    default Set<String> availableActions(Snapshot snapshot, ApprovalActor actor) {
        return ApprovalPolicy.availableActions(snapshot.status(), snapshot.ownerActorId(), actor).stream()
                .map(action -> action.apiName())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    default List<AvailableAction> availableActionDetails(Snapshot snapshot, ApprovalActor actor) {
        return availableActions(snapshot, actor).stream()
                .sorted()
                .map(name -> new AvailableAction(name, name, false))
                .toList();
    }
}
