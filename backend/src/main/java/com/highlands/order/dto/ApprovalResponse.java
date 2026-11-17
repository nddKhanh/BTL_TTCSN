package com.highlands.order.dto;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApprovalResponse(
        String approvalRef,
        ApprovalStatus status,
        String workflowState,
        long version,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        List<String> availableActions,
        List<WorkflowActionMetadata> availableActionMetadata
) {
    public static ApprovalResponse from(
            ApprovalPort.Snapshot snapshot,
            List<ApprovalPort.AvailableAction> actions
    ) {
        return new ApprovalResponse(
                snapshot.approvalRef(),
                snapshot.status(),
                snapshot.workflowState(),
                snapshot.version(),
                snapshot.createdAt(),
                snapshot.decidedAt(),
                actions.stream().map(ApprovalPort.AvailableAction::name).toList(),
                actions.stream().map(WorkflowActionMetadata::from).toList()
        );
    }
}
