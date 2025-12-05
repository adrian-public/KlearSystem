package com.klear.model.response;

public class ClearingResponse {
    private String orderId;
    private double nettedAmount;
    private String message;

    public ClearingResponse(String orderId, double nettedAmount, String message) {
        this.orderId = orderId;
        this.nettedAmount = nettedAmount;
        this.message = message;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getNettedAmount() {
        return nettedAmount;
    }

    public void setNettedAmount(double nettedAmount) {
        this.nettedAmount = nettedAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
