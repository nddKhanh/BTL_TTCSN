package com.highlands.order.domain.approval;

public enum ApprovalStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    public boolean isTerminal() {
        return this != PENDING;
    }
}
