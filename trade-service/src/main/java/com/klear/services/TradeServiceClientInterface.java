package com.klear.services;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;

public interface TradeServiceClientInterface {
    String submitOrder(Order order);
    OrderStatus getOrderStatus(String orderId);
}
