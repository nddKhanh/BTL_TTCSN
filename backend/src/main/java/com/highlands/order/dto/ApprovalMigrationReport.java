package com.highlands.order.dto;

import java.util.List;
import java.util.Map;

public record ApprovalMigrationReport(
        String phase,
        long localApprovals,
        long dxosApprovals,
        int eligible,
        int migrated,
        int failed,
        Map<String, Long> eligibleByStatus,
        List<String> issues,
        List<ApprovalMigrationItem> items
) {
    public ApprovalMigrationReport {
        eligibleByStatus = Map.copyOf(eligibleByStatus);
        issues = List.copyOf(issues);
        items = List.copyOf(items);
    }

    public record ApprovalMigrationItem(
            Long orderId,
            String orderCode,
            String outcome,
            String localApprovalRef,
            String dxosApprovalRef,
            String message
    ) {
    }
}
