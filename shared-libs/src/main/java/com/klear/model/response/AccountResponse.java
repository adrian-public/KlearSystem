package com.klear.model.response;

public class AccountResponse {
    private String orderId;
    private String message;
    public AccountResponse(String orderId, String message) {
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
