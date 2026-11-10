package com.highlands.order.infrastructure.approval;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.*;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.ApprovalHistory;
import com.highlands.order.model.OrderApproval;
import com.highlands.order.repository.ApprovalHistoryRepository;
import com.highlands.order.repository.OrderApprovalRepository;
import com.highlands.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.approval", name = "provider", havingValue = "local", matchIfMissing = true)
@Transactional(readOnly = true)
public class LocalApprovalAdapter implements ApprovalPort {

    private final OrderApprovalRepository approvals;
    private final ApprovalHistoryRepository history;
    private final OrderRepository orders;

    public LocalApprovalAdapter(
            OrderApprovalRepository approvals,
            ApprovalHistoryRepository history,
            OrderRepository orders
    ) {
        this.approvals = approvals;
        this.history = history;
        this.orders = orders;
    }

    @Override
    @Transactional
    public Snapshot submit(Submission submission, ApprovalActor actor, String requestId) {
        var existing = approvals.findByOrder_Id(submission.orderId());
        if (existing.isPresent()) {
            ApprovalHistory previous = history.findByApproval_IdAndRequestId(existing.get().getId(), requestId)
                    .orElseThrow(() -> conflict("Đơn hàng đã có yêu cầu xét duyệt"));
            if (!"SUBMIT".equals(previous.getAction())) {
                throw conflict("Request ID đã được dùng cho hành động khác");
            }
            return snapshot(existing.get());
        }

        OrderApproval approval = OrderApproval.builder()
                .approvalRef(UUID.randomUUID().toString())
                .order(orders.getReferenceById(submission.orderId()))
                .status(ApprovalStatus.PENDING)
                .createdBy(actor.actorId())
                .createdAt(LocalDateTime.now())
                .build();
        approval = approvals.saveAndFlush(approval);
        history.save(ApprovalHistory.builder()
                .approval(approval)
                .action("SUBMIT")
                .actorId(actor.actorId())
                .toStatus(ApprovalStatus.PENDING)
                .requestId(requestId)
                .build());
        return snapshot(approval);
    }

    @Override
    public Snapshot getByOrderId(Long orderId, ApprovalActor actor) {
        OrderApproval approval = approvals.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt của đơn hàng"));
        ensureCanRead(approval, actor);
        return snapshot(approval);
    }

    @Override
    public Map<Long, Snapshot> findByOrderIds(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return approvals.findByOrder_IdIn(orderIds).stream()
                .map(this::snapshot)
                .collect(Collectors.toMap(Snapshot::orderId, Function.identity()));
    }

    @Override
    public List<AvailableAction> availableActionDetails(Snapshot snapshot, ApprovalActor actor) {
        return availableActions(snapshot, actor).stream()
                .sorted()
                .map(name -> switch (name) {
                    case "approve" -> new AvailableAction(name, "Chấp nhận", false);
                    case "reject" -> new AvailableAction(name, "Từ chối", true);
                    case "withdraw" -> new AvailableAction(name, "Rút yêu cầu", false);
                    default -> new AvailableAction(name, name, false);
                })
                .toList();
    }

    @Override
    @Transactional
    public Snapshot execute(
            String approvalRef,
            String action,
            ApprovalActor actor,
            String reason,
            String requestId,
            long expectedVersion
    ) {
        OrderApproval approval = approvals.findByApprovalRef(approvalRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt"));
        ApprovalAction localAction = ApprovalAction.fromApiName(action);
        var previous = history.findByApproval_IdAndRequestId(approval.getId(), requestId);
        if (previous.isPresent()) {
            if (!localAction.name().equals(previous.get().getAction())) {
                throw conflict("Request ID đã được dùng cho hành động khác");
            }
            return snapshot(approval);
        }

        if (approval.getVersion() != expectedVersion) {
            throw conflict("Yêu cầu xét duyệt đã thay đổi; hãy tải lại dữ liệu");
        }
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw conflict("Yêu cầu xét duyệt đã kết thúc");
        }
        if (!availableActions(snapshot(approval), actor).contains(localAction.apiName())) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện hành động xét duyệt này");
        }
        if (localAction == ApprovalAction.REJECT && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Từ chối đơn hàng phải có lý do");
        }

        ApprovalStatus fromStatus = approval.getStatus();
        ApprovalStatus toStatus = switch (localAction) {
            case APPROVE -> ApprovalStatus.ACCEPTED;
            case REJECT -> ApprovalStatus.REJECTED;
            case WITHDRAW -> ApprovalStatus.WITHDRAWN;
        };
        approval.setStatus(toStatus);
        approval.setDecidedBy(actor.actorId());
        approval.setDecidedAt(LocalDateTime.now());
        approval = approvals.saveAndFlush(approval);
        history.save(ApprovalHistory.builder()
                .approval(approval)
                .action(localAction.name())
                .actorId(actor.actorId())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(normalizeReason(reason))
                .requestId(requestId)
                .build());
        return snapshot(approval);
    }

    @Override
    public java.util.List<HistoryEntry> history(String approvalRef, ApprovalActor actor) {
        OrderApproval approval = approvals.findByApprovalRef(approvalRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt"));
        ensureCanRead(approval, actor);
        return history.findByApproval_IdOrderByCreatedAtAscIdAsc(approval.getId()).stream()
                .map(item -> new HistoryEntry(
                        item.getAction(),
                        item.getActorId(),
                        item.getFromStatus() == null ? null : item.getFromStatus().name(),
                        item.getToStatus().name(),
                        item.getReason(),
                        item.getCreatedAt(),
                        item.getRequestId()
                ))
                .toList();
    }

    private void ensureCanRead(OrderApproval approval, ApprovalActor actor) {
        if (!ApprovalPolicy.canRead(approval.getCreatedBy(), actor)) {
            throw new AccessDeniedException("Bạn không có quyền xem yêu cầu xét duyệt này");
        }
    }

    private Snapshot snapshot(OrderApproval approval) {
        return new Snapshot(
                approval.getApprovalRef(),
                approval.getOrder().getId(),
                approval.getStatus(),
                approval.getStatus().name().toLowerCase(java.util.Locale.ROOT),
                approval.getCreatedBy(),
                approval.getDecidedBy(),
                approval.getCreatedAt(),
                approval.getDecidedAt(),
                approval.getVersion() == null ? 0 : approval.getVersion()
        );
    }

    private ApiDomainException conflict(String message) {
        return new ApiDomainException(ApiErrorCode.WORKFLOW_CONFLICT, HttpStatus.CONFLICT, message);
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
