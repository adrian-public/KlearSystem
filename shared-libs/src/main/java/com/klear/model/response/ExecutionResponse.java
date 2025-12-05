package com.klear.model.response;

public class ExecutionResponse {
    private String orderId;
    private double executedPrice;
    private long timestamp;

    public ExecutionResponse(String orderId, double executedPrice, long timestamp) {
        this.orderId = orderId;
        this.executedPrice = executedPrice;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getExecutedPrice() {
        return executedPrice;
    }

    public void setExecutedPrice(double executedPrice) {
        this.executedPrice = executedPrice;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
