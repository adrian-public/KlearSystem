package com.klear.execution.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionServiceTest {

    private ExecutionService executionService;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        executionService = new ExecutionService();

        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testProcessTrade_ExecutesOrder() throws Exception {
        Trade trade = new Trade("ORDER-001", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);

        long beforeExecution = System.currentTimeMillis();
        Trade result = (Trade) processTrade.invoke(executionService, trade);
        long afterExecution = System.currentTimeMillis();

        assertEquals(OrderStatus.EXECUTED, result.getStatus());
        assertEquals(150.00, result.getExecutedPrice(), 0.001); // Simulated price equals order price
        assertTrue(result.getExecutedTimestamp() >= beforeExecution);
        assertTrue(result.getExecutedTimestamp() <= afterExecution);
    }

    @Test
    void testProcessTrade_SetsExecutedPrice() throws Exception {
        testOrder.setPrice(175.50);
        Trade trade = new Trade("ORDER-002", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertEquals(175.50, result.getExecutedPrice(), 0.001);
    }

    @Test
    void testProcessTrade_SetsTimestamp() throws Exception {
        Trade trade = new Trade("ORDER-003", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertTrue(result.getExecutedTimestamp() > 0);
    }

    @Test
    void testGetServiceName() {
        assertEquals("ExecutionService", executionService.getServiceName());
    }

    @Test
    void testProcessTrade_FailsOnZeroPrice() throws Exception {
        testOrder.setPrice(0);
        Trade trade = new Trade("ORDER-004", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("EXECUTION", result.getFailureStage());
        assertTrue(result.getFailureReason().contains("Invalid price"));
    }

    @Test
    void testProcessTrade_FailsOnNegativePrice() throws Exception {
        testOrder.setPrice(-10.0);
        Trade trade = new Trade("ORDER-005", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("EXECUTION", result.getFailureStage());
        assertTrue(result.getFailureReason().contains("Invalid price"));
    }

    @Test
    void testProcessTrade_FailsOnZeroQuantity() throws Exception {
        testOrder.setQuantity(0);
        Trade trade = new Trade("ORDER-006", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("EXECUTION", result.getFailureStage());
        assertTrue(result.getFailureReason().contains("Invalid quantity"));
    }

    @Test
    void testProcessTrade_FailsOnNegativeQuantity() throws Exception {
        testOrder.setQuantity(-100);
        Trade trade = new Trade("ORDER-007", testOrder, OrderStatus.VALIDATED);

        Method processTrade = ExecutionService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(executionService, trade);

        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("EXECUTION", result.getFailureStage());
        assertTrue(result.getFailureReason().contains("Invalid quantity"));
    }
}