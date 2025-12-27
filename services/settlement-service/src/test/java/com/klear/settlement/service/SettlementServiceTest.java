package com.klear.settlement.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SettlementServiceTest {

    private SettlementService settlementService;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService();

        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testProcessTrade_SettlesOrder() throws Exception {
        Trade trade = new Trade("ORDER-001", testOrder, OrderStatus.CLEARED);
        trade.setNettedAmount(15000.00);

        Method processTrade = SettlementService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(settlementService, trade);

        assertEquals(OrderStatus.SETTLED, result.getStatus());
        assertEquals("Settlement Successful", result.getSettlementMessage());
    }

    @Test
    void testProcessTrade_PreservesNettedAmount() throws Exception {
        Trade trade = new Trade("ORDER-002", testOrder, OrderStatus.CLEARED);
        trade.setNettedAmount(25000.00);

        Method processTrade = SettlementService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(settlementService, trade);

        assertEquals(25000.00, result.getNettedAmount(), 0.001);
    }

    @Test
    void testGetServiceName() {
        assertEquals("SettlementService", settlementService.getServiceName());
    }
}