package com.klear.model.trade;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;

public class Trade {

    private String orderId;
    private Order order;
    private double executedPrice;
    private long executedTimestamp;
    private double nettedAmount;
    private OrderStatus status;
    private String ValidationMessage = "";
    private String ClearingMessage = "";
    private String SettlementMessage = "";

    public Trade(){}

    public Trade(String orderId, Order order, OrderStatus status) {
        this.orderId = orderId;
        this.order = order;
        this.executedPrice = 0.0;
        this.executedTimestamp = 0;
        this.nettedAmount = 0.0;
        this.status = status;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public double getExecutedPrice() {
        return executedPrice;
    }

    public void setExecutedPrice(double executedPrice) {
        this.executedPrice = executedPrice;
    }

    public double getNettedAmount() {
        return nettedAmount;
    }

    public void setNettedAmount(double nettedAmount) {
        this.nettedAmount = nettedAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getValidationMessage() {
        return ValidationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        ValidationMessage = validationMessage;
    }

    public String getClearingMessage() {
        return ClearingMessage;
    }

    public void setClearingMessage(String clearingMessage) {
        ClearingMessage = clearingMessage;
    }

    public String getSettlementMessage() {
        return SettlementMessage;
    }

    public void setSettlementMessage(String settlementMessage) {
        SettlementMessage = settlementMessage;
    }

    public long getExecutedTimestamp() {
        return executedTimestamp;
    }

    public void setExecutedTimestamp(long executedTimestamp) {
        this.executedTimestamp = executedTimestamp;
    }
}
