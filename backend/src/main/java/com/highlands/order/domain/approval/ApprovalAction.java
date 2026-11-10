package com.highlands.order.domain.approval;

import java.util.Locale;

public enum ApprovalAction {
    APPROVE("approve"),
    REJECT("reject"),
    WITHDRAW("withdraw");

    private final String apiName;

    ApprovalAction(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    public static ApprovalAction fromApiName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Hành động xét duyệt không hợp lệ");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ApprovalAction action : values()) {
            if (action.apiName.equals(normalized)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Hành động xét duyệt không hợp lệ: " + value);
    }
}
