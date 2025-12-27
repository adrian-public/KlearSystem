package com.klear.clearing.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ClearingServiceTest {

    private ClearingService clearingService;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        clearingService = new ClearingService();

        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testProcessTrade_ClearsOrder() throws Exception {
        Trade trade = new Trade("ORDER-001", testOrder, OrderStatus.EXECUTED);
        trade.setExecutedPrice(151.50);

        Method processTrade = ClearingService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(clearingService, trade);

        assertEquals(OrderStatus.CLEARED, result.getStatus());
        assertEquals("Clearing Successful", result.getClearingMessage());
    }

    @Test
    void testProcessTrade_CalculatesNettedAmount() throws Exception {
        testOrder.setQuantity(100);
        Trade trade = new Trade("ORDER-002", testOrder, OrderStatus.EXECUTED);
        trade.setExecutedPrice(150.00);

        Method processTrade = ClearingService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(clearingService, trade);

        // nettedAmount = quantity * executedPrice = 100 * 150.00 = 15000.00
        assertEquals(15000.00, result.getNettedAmount(), 0.001);
    }

    @Test
    void testProcessTrade_CalculatesNettedAmount_DifferentValues() throws Exception {
        testOrder.setQuantity(50);
        Trade trade = new Trade("ORDER-003", testOrder, OrderStatus.EXECUTED);
        trade.setExecutedPrice(200.00);

        Method processTrade = ClearingService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(clearingService, trade);

        // nettedAmount = quantity * executedPrice = 50 * 200.00 = 10000.00
        assertEquals(10000.00, result.getNettedAmount(), 0.001);
    }

    @Test
    void testGetServiceName() {
        assertEquals("ClearingService", clearingService.getServiceName());
    }
}