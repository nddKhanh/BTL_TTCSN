package com.highlands.order.dto;

import com.highlands.order.application.approval.ApprovalPort;

public record WorkflowActionMetadata(
        String name,
        String label,
        boolean reasonRequired
) {
    public static WorkflowActionMetadata from(ApprovalPort.AvailableAction action) {
        return new WorkflowActionMetadata(action.name(), action.label(), action.reasonRequired());
    }
}
