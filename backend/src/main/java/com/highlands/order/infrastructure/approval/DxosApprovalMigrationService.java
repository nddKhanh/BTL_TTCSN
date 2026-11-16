package com.highlands.order.infrastructure.approval;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalProvider;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.dto.ApprovalMigrationReport;
import com.highlands.order.dto.ApprovalMigrationReport.ApprovalMigrationItem;
import com.highlands.order.model.ApprovalHistory;
import com.highlands.order.model.OrderApproval;
import com.highlands.order.repository.ApprovalHistoryRepository;
import com.highlands.order.repository.OrderApprovalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "app.approval", name = "provider", havingValue = "dxos")
public class DxosApprovalMigrationService {

    private static final String MIGRATION_NOTE = "Chuyển đổi từ TTCSN Local sang DX-OS";

    private final DxosApiClient client;
    private final OrderApprovalRepository approvals;
    private final ApprovalHistoryRepository history;
    private final TransactionTemplate transactions;

    public DxosApprovalMigrationService(
            DxosApiClient client,
            OrderApprovalRepository approvals,
            ApprovalHistoryRepository history,
            TransactionTemplate transactions
    ) {
        this.client = client;
        this.approvals = approvals;
        this.history = history;
        this.transactions = transactions;
    }

    public ApprovalMigrationReport preflight() {
        List<Candidate> candidates = candidates();
        DxosApiClient.EntityMetadata metadata = client.metadata(DxosApiClient.RemoteIdentity.APPROVER);
        List<String> issues = validatePaths(candidates, metadata);
        return report("PREFLIGHT", candidates, 0, 0, issues, List.of());
    }

    public synchronized ApprovalMigrationReport migrate() {
        List<Candidate> candidates = candidates();
        DxosApiClient.EntityMetadata metadata = client.metadata(DxosApiClient.RemoteIdentity.APPROVER);
        List<String> issues = validatePaths(candidates, metadata);
        if (!issues.isEmpty()) {
            return report("MIGRATION", candidates, 0, candidates.size(), issues, List.of());
        }

        List<ApprovalMigrationItem> items = new ArrayList<>();
        int migrated = 0;
        for (Candidate candidate : candidates) {
            try {
                String remoteId = migrate(candidate, metadata);
                migrated++;
                items.add(new ApprovalMigrationItem(
                        candidate.orderId(), candidate.orderCode(), "MIGRATED",
                        candidate.localApprovalRef(), remoteId, "Đã chuyển và đối chiếu trạng thái"
                ));
            } catch (RuntimeException exception) {
                items.add(new ApprovalMigrationItem(
                        candidate.orderId(), candidate.orderCode(), "FAILED",
                        candidate.localApprovalRef(), candidate.remoteApprovalRef(), safeMessage(exception)
                ));
            }
        }
        return report("MIGRATION", candidates, migrated, candidates.size() - migrated, List.of(), items);
    }

    private String migrate(Candidate candidate, DxosApiClient.EntityMetadata metadata) {
        LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC);
        DxosApiClient.RecordData remote;
        if (candidate.remoteApprovalRef() == null) {
            remote = client.create(new ApprovalPort.Submission(
                    candidate.orderId(),
                    candidate.orderCode(),
                    candidate.customerName(),
                    candidate.totalAmount()
            ));
            linkRemoteRecord(candidate, remote.id());
        } else {
            remote = client.get(candidate.remoteApprovalRef(), DxosApiClient.RemoteIdentity.APPROVER);
        }

        String target = targetState(candidate.targetStatus());
        for (DxosApiClient.ActionMetadata action : path(remote.status(), target, metadata)) {
            DxosApiClient.RemoteIdentity identity = "OWNER".equalsIgnoreCase(action.by())
                    ? DxosApiClient.RemoteIdentity.REQUESTER
                    : DxosApiClient.RemoteIdentity.APPROVER;
            remote = client.act(remote.id(), action.name(), MIGRATION_NOTE, identity);
        }
        if (!target.equalsIgnoreCase(remote.status())) {
            throw new IllegalStateException("DX-OS chưa đạt trạng thái đích " + target);
        }
        complete(candidate, remote, startedAt);
        return remote.id();
    }

    private void linkRemoteRecord(Candidate candidate, String remoteId) {
        transactions.executeWithoutResult(status -> {
            OrderApproval approval = approvals.findById(candidate.approvalId())
                    .orElseThrow(() -> new IllegalStateException("Approval không còn tồn tại"));
            if (approval.getProvider() != ApprovalProvider.LOCAL
                    || !approval.getApprovalRef().equals(candidate.localApprovalRef())) {
                throw new IllegalStateException("Approval đã được tiến trình khác thay đổi");
            }
            approval.setLocalApprovalRef(candidate.localApprovalRef());
            approval.setApprovalRef(remoteId);
            approval.setProvider(ApprovalProvider.DXOS);
            approvals.saveAndFlush(approval);
        });
    }

    private void complete(Candidate candidate, DxosApiClient.RecordData remote, LocalDateTime startedAt) {
        transactions.executeWithoutResult(status -> {
            OrderApproval approval = approvals.findById(candidate.approvalId())
                    .orElseThrow(() -> new IllegalStateException("Approval không còn tồn tại"));
            if (approval.getProvider() != ApprovalProvider.DXOS
                    || !approval.getApprovalRef().equals(remote.id())) {
                throw new IllegalStateException("Liên kết DX-OS đã bị thay đổi");
            }
            ApprovalStatus resultStatus = status(remote.status());
            approval.setStatus(resultStatus);
            approval.setDecidedBy(remote.decidedBy());
            approval.setDecidedAt(local(remote.decidedAt()));
            approval.setMigrationCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
            approval = approvals.saveAndFlush(approval);
            if (history.findByApproval_IdAndRequestId(approval.getId(), requestId(approval.getId())).isEmpty()) {
                history.save(ApprovalHistory.builder()
                        .approval(approval)
                        .action("MIGRATE_TO_DXOS")
                        .actorId("migration")
                        .fromStatus(candidate.targetStatus())
                        .toStatus(resultStatus)
                        .reason(MIGRATION_NOTE + ": " + candidate.localApprovalRef() + " -> " + remote.id())
                        .createdAt(startedAt)
                        .requestId(requestId(approval.getId()))
                        .build());
            }
        });
    }

    private List<Candidate> candidates() {
        List<OrderApproval> source = new ArrayList<>(approvals.findByProviderOrderById(ApprovalProvider.LOCAL));
        source.addAll(approvals
                .findByProviderAndLocalApprovalRefIsNotNullAndMigrationCompletedAtIsNullOrderById(
                        ApprovalProvider.DXOS));
        return source.stream().map(this::candidate).toList();
    }

    private Candidate candidate(OrderApproval approval) {
        boolean linked = approval.getProvider() == ApprovalProvider.DXOS;
        return new Candidate(
                approval.getId(),
                approval.getOrder().getId(),
                approval.getOrder().getOrderCode(),
                approval.getOrder().getCustomerName(),
                approval.getOrder().getTotalAmount(),
                approval.getStatus(),
                linked ? approval.getLocalApprovalRef() : approval.getApprovalRef(),
                linked ? approval.getApprovalRef() : null
        );
    }

    private List<String> validatePaths(
            List<Candidate> candidates,
            DxosApiClient.EntityMetadata metadata
    ) {
        if (metadata.process() == null) {
            return List.of("Model DX-OS không có workflow process");
        }
        List<String> issues = new ArrayList<>();
        Set<ApprovalStatus> statuses = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (statuses.add(candidate.targetStatus())) {
                try {
                    path("pending", targetState(candidate.targetStatus()), metadata);
                } catch (IllegalStateException exception) {
                    issues.add(exception.getMessage());
                }
            }
        }
        return List.copyOf(issues);
    }

    private List<DxosApiClient.ActionMetadata> path(
            String from,
            String target,
            DxosApiClient.EntityMetadata metadata
    ) {
        if (from.equalsIgnoreCase(target)) {
            return List.of();
        }
        ArrayDeque<PathNode> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new PathNode(from.toLowerCase(Locale.ROOT), List.of()));
        while (!queue.isEmpty()) {
            PathNode node = queue.removeFirst();
            if (!visited.add(node.state())) {
                continue;
            }
            for (DxosApiClient.ActionMetadata action : metadata.process().actions()) {
                if (action.to() == null || action.from().stream().noneMatch(node.state()::equalsIgnoreCase)) {
                    continue;
                }
                List<DxosApiClient.ActionMetadata> nextPath = new ArrayList<>(node.actions());
                nextPath.add(action);
                if (target.equalsIgnoreCase(action.to())) {
                    return List.copyOf(nextPath);
                }
                queue.addLast(new PathNode(action.to().toLowerCase(Locale.ROOT), List.copyOf(nextPath)));
            }
        }
        throw new IllegalStateException("Workflow DX-OS không có đường chuyển từ " + from + " đến " + target);
    }

    private ApprovalMigrationReport report(
            String phase,
            List<Candidate> candidates,
            int migrated,
            int failed,
            List<String> issues,
            List<ApprovalMigrationItem> items
    ) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<ApprovalStatus, Long> counts = new EnumMap<>(ApprovalStatus.class);
        for (Candidate candidate : candidates) {
            counts.merge(candidate.targetStatus(), 1L, Long::sum);
        }
        counts.forEach((key, value) -> byStatus.put(key.name(), value));
        return new ApprovalMigrationReport(
                phase,
                approvals.countByProvider(ApprovalProvider.LOCAL),
                approvals.countByProvider(ApprovalProvider.DXOS),
                candidates.size(),
                migrated,
                failed,
                byStatus,
                issues,
                items
        );
    }

    private String targetState(ApprovalStatus status) {
        return switch (status) {
            case PENDING -> "pending";
            case ACCEPTED -> "accepted";
            case REJECTED -> "rejected";
            case WITHDRAWN -> "withdrawn";
        };
    }

    private ApprovalStatus status(String workflowState) {
        return switch (workflowState.toLowerCase(Locale.ROOT)) {
            case "pending", "reviewing" -> ApprovalStatus.PENDING;
            case "accepted" -> ApprovalStatus.ACCEPTED;
            case "rejected" -> ApprovalStatus.REJECTED;
            case "withdrawn" -> ApprovalStatus.WITHDRAWN;
            default -> throw new IllegalStateException("Trạng thái DX-OS không hỗ trợ: " + workflowState);
        };
    }

    private LocalDateTime local(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String requestId(Long approvalId) {
        return "dxos-migration:" + approvalId;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record Candidate(
            Long approvalId,
            Long orderId,
            String orderCode,
            String customerName,
            BigDecimal totalAmount,
            ApprovalStatus targetStatus,
            String localApprovalRef,
            String remoteApprovalRef
    ) {
    }

    private record PathNode(String state, List<DxosApiClient.ActionMetadata> actions) {
    }
}
