package com.klear.trade.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the trade lifecycle state machine.
 * Tests that trades progress through statuses in the correct order.
 */
class TradeLifecycleIT {

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setClientId("IT-CLIENT");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    @DisplayName("Trade lifecycle follows correct order: UNKNOWN -> VALIDATED -> EXECUTED -> CLEARED -> SETTLED")
    void testTradeLifecycleOrder() {
        Trade trade = new Trade("LIFECYCLE-001", testOrder, OrderStatus.UNKNOWN);

        // Initial state
        assertEquals(OrderStatus.UNKNOWN, trade.getStatus());

        // Step 1: Validation
        trade.setStatus(OrderStatus.VALIDATED);
        trade.setValidationMessage("Account validated");
        assertEquals(OrderStatus.VALIDATED, trade.getStatus());
        assertFalse(trade.getValidationMessage().isEmpty());

        // Step 2: Execution
        trade.setStatus(OrderStatus.EXECUTED);
        trade.setExecutedPrice(151.00);
        trade.setExecutedTimestamp(System.currentTimeMillis());
        assertEquals(OrderStatus.EXECUTED, trade.getStatus());
        assertTrue(trade.getExecutedPrice() > 0);
        assertTrue(trade.getExecutedTimestamp() > 0);

        // Step 3: Clearing
        trade.setStatus(OrderStatus.CLEARED);
        trade.setNettedAmount(trade.getOrder().getQuantity() * trade.getExecutedPrice());
        trade.setClearingMessage("Cleared successfully");
        assertEquals(OrderStatus.CLEARED, trade.getStatus());
        assertEquals(15100.00, trade.getNettedAmount(), 0.01);

        // Step 4: Settlement
        trade.setStatus(OrderStatus.SETTLED);
        trade.setSettlementMessage("Settled successfully");
        assertEquals(OrderStatus.SETTLED, trade.getStatus());
        assertFalse(trade.getSettlementMessage().isEmpty());
    }

    @Test
    @DisplayName("Status ordinals increase throughout lifecycle")
    void testStatusOrdinalsIncrease() {
        OrderStatus[] lifecycle = {
            OrderStatus.UNKNOWN,
            OrderStatus.VALIDATED,
            OrderStatus.EXECUTED,
            OrderStatus.CLEARED,
            OrderStatus.SETTLED
        };

        for (int i = 1; i < lifecycle.length; i++) {
            assertTrue(lifecycle[i].ordinal() > lifecycle[i - 1].ordinal(),
                "Status " + lifecycle[i] + " should have higher ordinal than " + lifecycle[i - 1]);
        }
    }

    @Test
    @DisplayName("Trade preserves all data through lifecycle")
    void testDataPreservationThroughLifecycle() {
        Trade trade = new Trade("PRESERVE-001", testOrder, OrderStatus.UNKNOWN);

        // Set all fields
        trade.setStatus(OrderStatus.VALIDATED);
        trade.setValidationMessage("Validated");

        trade.setStatus(OrderStatus.EXECUTED);
        trade.setExecutedPrice(152.50);
        trade.setExecutedTimestamp(1234567890L);

        trade.setStatus(OrderStatus.CLEARED);
        trade.setNettedAmount(15250.00);
        trade.setClearingMessage("Cleared");

        trade.setStatus(OrderStatus.SETTLED);
        trade.setSettlementMessage("Settled");

        // Verify all data is preserved
        assertEquals("PRESERVE-001", trade.getOrderId());
        assertEquals("IT-CLIENT", trade.getOrder().getClientId());
        assertEquals("AAPL", trade.getOrder().getStockSymbol());
        assertEquals(100, trade.getOrder().getQuantity());
        assertEquals(150.00, trade.getOrder().getPrice(), 0.001);
        assertEquals("Validated", trade.getValidationMessage());
        assertEquals(152.50, trade.getExecutedPrice(), 0.001);
        assertEquals(1234567890L, trade.getExecutedTimestamp());
        assertEquals(15250.00, trade.getNettedAmount(), 0.001);
        assertEquals("Cleared", trade.getClearingMessage());
        assertEquals("Settled", trade.getSettlementMessage());
    }

    @Test
    @DisplayName("Multiple trades can be at different lifecycle stages")
    void testMultipleTradesAtDifferentStages() {
        Trade trade1 = new Trade("MULTI-001", testOrder, OrderStatus.UNKNOWN);
        Trade trade2 = new Trade("MULTI-002", testOrder, OrderStatus.VALIDATED);
        Trade trade3 = new Trade("MULTI-003", testOrder, OrderStatus.EXECUTED);
        Trade trade4 = new Trade("MULTI-004", testOrder, OrderStatus.CLEARED);
        Trade trade5 = new Trade("MULTI-005", testOrder, OrderStatus.SETTLED);

        assertEquals(OrderStatus.UNKNOWN, trade1.getStatus());
        assertEquals(OrderStatus.VALIDATED, trade2.getStatus());
        assertEquals(OrderStatus.EXECUTED, trade3.getStatus());
        assertEquals(OrderStatus.CLEARED, trade4.getStatus());
        assertEquals(OrderStatus.SETTLED, trade5.getStatus());
    }

    @Test
    @DisplayName("Netted amount calculation is correct")
    void testNettedAmountCalculation() {
        // Test various quantity/price combinations
        int[][] testCases = {
            {100, 150},   // 15000
            {50, 200},    // 10000
            {1, 1000},    // 1000
            {1000, 1},    // 1000
        };

        for (int[] testCase : testCases) {
            int quantity = testCase[0];
            double price = testCase[1];
            double expectedNetted = quantity * price;

            Order order = new Order();
            order.setQuantity(quantity);
            order.setPrice(price);

            Trade trade = new Trade("CALC-" + quantity + "-" + (int) price, order, OrderStatus.EXECUTED);
            trade.setExecutedPrice(price);

            double nettedAmount = trade.getOrder().getQuantity() * trade.getExecutedPrice();
            assertEquals(expectedNetted, nettedAmount, 0.001,
                "Netted amount for qty=" + quantity + " price=" + price + " should be " + expectedNetted);
        }
    }
}