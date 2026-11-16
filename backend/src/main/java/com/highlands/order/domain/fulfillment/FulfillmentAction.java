package com.highlands.order.domain.fulfillment;

public enum FulfillmentAction {
    START_PREPARATION("startPreparation"),
    DISPATCH("dispatch"),
    COMPLETE("complete"),
    CANCEL("cancel");

    private final String apiName;

    FulfillmentAction(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    public static FulfillmentAction fromApiName(String value) {
        if (value != null) {
            for (FulfillmentAction action : values()) {
                if (action.apiName.equalsIgnoreCase(value.trim())) {
                    return action;
                }
            }
        }
        throw new IllegalArgumentException("Hành động thực hiện đơn không hợp lệ: " + value);
    }
}
