package com.highlands.order.infrastructure.approval;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.approval.ApprovalProvider;
import com.highlands.order.domain.approval.ApprovalPolicy;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.ApprovalHistory;
import com.highlands.order.model.OrderApproval;
import com.highlands.order.repository.ApprovalHistoryRepository;
import com.highlands.order.repository.OrderApprovalRepository;
import com.highlands.order.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.approval", name = "provider", havingValue = "dxos")
@Transactional(readOnly = true)
public class DxosApprovalAdapter implements ApprovalPort {

    private final DxosApiClient client;
    private final OrderApprovalRepository approvals;
    private final ApprovalHistoryRepository history;
    private final OrderRepository orders;

    public DxosApprovalAdapter(
            DxosApiClient client,
            OrderApprovalRepository approvals,
            ApprovalHistoryRepository history,
            OrderRepository orders
    ) {
        this.client = client;
        this.approvals = approvals;
        this.history = history;
        this.orders = orders;
    }

    @Override
    @Transactional
    public Snapshot submit(Submission submission, ApprovalActor actor, String requestId) {
        if (!actor.authenticated()) {
            throw new AccessDeniedException("DX-OS chỉ nhận yêu cầu từ người dùng đã đăng nhập");
        }
        var existing = approvals.findByOrder_Id(submission.orderId());
        if (existing.isPresent()) {
            requireDxos(existing.get());
            ApprovalHistory previous = history.findByApproval_IdAndRequestId(existing.get().getId(), requestId)
                    .orElseThrow(() -> conflict("Đơn hàng đã có yêu cầu xét duyệt"));
            if (!"SUBMIT".equals(previous.getAction())) {
                throw conflict("Request ID đã được dùng cho hành động khác");
            }
            return snapshot(existing.get(), client.get(
                    existing.get().getApprovalRef(),
                    DxosApiClient.RemoteIdentity.REQUESTER
            ));
        }

        DxosApiClient.RecordData remote = client.create(submission);
        OrderApproval approval = OrderApproval.builder()
                .approvalRef(remote.id())
                .provider(ApprovalProvider.DXOS)
                .order(orders.getReferenceById(submission.orderId()))
                .status(status(remote.status()))
                .createdBy(actor.actorId())
                .createdAt(local(remote.createdAt()))
                .decidedBy(remote.decidedBy())
                .decidedAt(local(remote.decidedAt()))
                .build();
        approval = approvals.saveAndFlush(approval);
        history.save(ApprovalHistory.builder()
                .approval(approval)
                .action("SUBMIT")
                .actorId(actor.actorId())
                .toStatus(approval.getStatus())
                .requestId(requestId)
                .build());
        return snapshot(approval, remote);
    }

    @Override
    public Snapshot getByOrderId(Long orderId, ApprovalActor actor) {
        OrderApproval approval = approvals.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt của đơn hàng"));
        requireDxos(approval);
        ensureCanRead(approval, actor);
        return snapshot(approval, client.get(approval.getApprovalRef(), identity(approval, actor)));
    }

    @Override
    public Map<Long, Snapshot> findByOrderIds(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderApproval> found = approvals.findByOrder_IdIn(orderIds);
        found.forEach(this::requireDxos);
        return found.stream()
                .map(approval -> snapshot(
                        approval,
                        client.get(approval.getApprovalRef(), DxosApiClient.RemoteIdentity.APPROVER)
                ))
                .collect(Collectors.toMap(Snapshot::orderId, Function.identity()));
    }

    @Override
    public Set<String> availableActions(Snapshot snapshot, ApprovalActor actor) {
        return availableActionDetails(snapshot, actor).stream()
                .map(AvailableAction::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<AvailableAction> availableActionDetails(Snapshot snapshot, ApprovalActor actor) {
        if (snapshot.status().isTerminal()) {
            return List.of();
        }
        DxosApiClient.RemoteIdentity identity = identity(snapshot.ownerActorId(), actor);
        DxosApiClient.EntityMetadata metadata = client.metadata(identity);
        if (metadata.process() == null) {
            return List.of();
        }
        boolean owner = isOwner(snapshot.ownerActorId(), actor);
        Set<String> handlerActions = Set.copyOf(metadata.handlerActions());
        return metadata.process().actions().stream()
                .filter(action -> action.from().contains(snapshot.workflowState()))
                .filter(action -> owner
                        ? "OWNER".equalsIgnoreCase(action.by())
                        : actor.hasAuthority("ORDER_APPROVE")
                                && "HANDLER".equalsIgnoreCase(action.by())
                                && handlerActions.contains(action.name()))
                .map(action -> new AvailableAction(action.name(), action.label(), action.needsNote()))
                .sorted(java.util.Comparator.comparing(AvailableAction::name))
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
        if (action == null || !ACTION_NAME.matcher(action).matches()) {
            throw new IllegalArgumentException("Tên action không hợp lệ");
        }
        OrderApproval approval = approvals.findByApprovalRef(approvalRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt"));
        requireDxos(approval);
        ensureCanRead(approval, actor);
        var previous = history.findByApproval_IdAndRequestId(approval.getId(), requestId);
        DxosApiClient.RemoteIdentity identity = identity(approval, actor);
        if (previous.isPresent()) {
            if (!action.equals(previous.get().getAction())) {
                throw conflict("Request ID đã được dùng cho hành động khác");
            }
            return snapshot(approval, client.get(approvalRef, identity));
        }

        DxosApiClient.RecordData current = client.get(approvalRef, identity);
        if (current.version() != expectedVersion) {
            throw conflict("Yêu cầu xét duyệt đã thay đổi; hãy tải lại dữ liệu");
        }
        Snapshot currentSnapshot = snapshot(approval, current);
        AvailableAction selected = availableActionDetails(currentSnapshot, actor).stream()
                .filter(candidate -> candidate.name().equals(action))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Bạn không có quyền thực hiện action này ở trạng thái hiện tại"));
        if (selected.reasonRequired() && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Action này yêu cầu lý do");
        }

        DxosApiClient.RecordData result = client.act(approvalRef, action, normalizeReason(reason), identity);
        ApprovalStatus fromStatus = status(current.status());
        ApprovalStatus toStatus = status(result.status());
        approval.setStatus(toStatus);
        approval.setDecidedBy(result.decidedBy());
        approval.setDecidedAt(local(result.decidedAt()));
        approval = approvals.saveAndFlush(approval);
        history.save(ApprovalHistory.builder()
                .approval(approval)
                .action(action)
                .actorId(actor.actorId())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(normalizeReason(reason))
                .requestId(requestId)
                .build());
        return snapshot(approval, result);
    }

    @Override
    public List<HistoryEntry> history(String approvalRef, ApprovalActor actor) {
        OrderApproval approval = approvals.findByApprovalRef(approvalRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xét duyệt"));
        requireDxos(approval);
        ensureCanRead(approval, actor);
        List<HistoryEntry> combined = new ArrayList<>(history
                .findByApproval_IdOrderByCreatedAtAscIdAsc(approval.getId()).stream()
                .map(item -> new HistoryEntry(
                        item.getAction(),
                        item.getActorId(),
                        item.getFromStatus() == null ? null : item.getFromStatus().name().toLowerCase(Locale.ROOT),
                        item.getToStatus().name().toLowerCase(Locale.ROOT),
                        item.getReason(),
                        item.getCreatedAt(),
                        item.getRequestId()
                ))
                .toList());
        combined.addAll(client.events(approvalRef, identity(approval, actor)).stream()
                .map(event -> new HistoryEntry(
                        event.action(),
                        event.actor(),
                        event.fromState(),
                        event.toState(),
                        event.note(),
                        local(event.at()),
                        "dxos-event:" + event.id()
                ))
                .toList());
        combined.sort(Comparator.comparing(HistoryEntry::createdAt));
        return List.copyOf(combined);
    }

    private void ensureCanRead(OrderApproval approval, ApprovalActor actor) {
        if (!ApprovalPolicy.canRead(approval.getCreatedBy(), actor)) {
            throw new AccessDeniedException("Bạn không có quyền xem yêu cầu xét duyệt này");
        }
    }

    private void requireDxos(OrderApproval approval) {
        if (approval.getProvider() != ApprovalProvider.DXOS
                || (approval.getLocalApprovalRef() != null && approval.getMigrationCompletedAt() == null)) {
            throw new ApiDomainException(
                    ApiErrorCode.APPROVAL_MIGRATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "Approval cục bộ chưa được chuyển sang DX-OS"
            );
        }
    }

    private DxosApiClient.RemoteIdentity identity(OrderApproval approval, ApprovalActor actor) {
        return identity(approval.getCreatedBy(), actor);
    }

    private DxosApiClient.RemoteIdentity identity(String ownerActorId, ApprovalActor actor) {
        if (isOwner(ownerActorId, actor)) {
            return DxosApiClient.RemoteIdentity.REQUESTER;
        }
        if (actor.hasAuthority("ORDER_APPROVE") || actor.hasAuthority("ORDER_READ")) {
            return DxosApiClient.RemoteIdentity.APPROVER;
        }
        return DxosApiClient.RemoteIdentity.REQUESTER;
    }

    private boolean isOwner(String ownerActorId, ApprovalActor actor) {
        return actor.authenticated() && actor.actorId().equals(ownerActorId);
    }

    private Snapshot snapshot(OrderApproval approval, DxosApiClient.RecordData remote) {
        return new Snapshot(
                approval.getApprovalRef(),
                approval.getOrder().getId(),
                status(remote.status()),
                remote.status(),
                approval.getCreatedBy(),
                remote.decidedBy(),
                local(remote.createdAt()),
                local(remote.decidedAt()),
                remote.version()
        );
    }

    private ApprovalStatus status(String workflowState) {
        return switch (workflowState.toLowerCase(Locale.ROOT)) {
            case "pending", "reviewing" -> ApprovalStatus.PENDING;
            case "accepted" -> ApprovalStatus.ACCEPTED;
            case "rejected" -> ApprovalStatus.REJECTED;
            case "withdrawn" -> ApprovalStatus.WITHDRAWN;
            default -> throw new ApiDomainException(
                    ApiErrorCode.APPROVAL_PROVIDER_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "DX-OS trả workflow state chưa được ánh xạ: " + workflowState
            );
        };
    }

    private LocalDateTime local(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private ApiDomainException conflict(String message) {
        return new ApiDomainException(ApiErrorCode.WORKFLOW_CONFLICT, HttpStatus.CONFLICT, message);
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
