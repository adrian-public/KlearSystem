package com.klear.model.response;

public class SettlementResponse {
    private String orderId;
    private String message;

    public SettlementResponse(String orderId, String message) {
        this.orderId = orderId;
        this.message = message;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
