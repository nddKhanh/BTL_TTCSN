package com.highlands.order.dto;

import java.time.LocalDateTime;

public record WorkflowHistoryResponse(
        String action,
        String actorId,
        String fromStatus,
        String toStatus,
        String reason,
        LocalDateTime createdAt,
        String requestId
) {
}
