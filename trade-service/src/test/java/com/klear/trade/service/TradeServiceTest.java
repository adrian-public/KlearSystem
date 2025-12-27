package com.klear.trade.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TradeServiceTest {

    private TradeService tradeService;
    private Order testOrder;

    @BeforeEach
    void setUp() throws Exception {
        tradeService = new TradeService();

        // Inject a mock trade status map using reflection
        Field mapField = TradeService.class.getDeclaredField("concurrentTradeStatusMap");
        mapField.setAccessible(true);
        mapField.set(tradeService, new ConcurrentHashMap<>());

        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testGetOrderStatus_UnknownOrder() {
        OrderStatus status = tradeService.getOrderStatus("NON-EXISTENT-ORDER");
        assertEquals(OrderStatus.UNKNOWN, status);
    }

    @Test
    void testGetOrderStatus_AfterStatusUpdate() throws Exception {
        // Access the internal map and add a trade directly
        Field mapField = TradeService.class.getDeclaredField("concurrentTradeStatusMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, com.klear.model.trade.Trade> map =
            (Map<String, com.klear.model.trade.Trade>) mapField.get(tradeService);

        com.klear.model.trade.Trade trade = new com.klear.model.trade.Trade(
            "TEST-ORDER-001", testOrder, OrderStatus.VALIDATED);
        map.put("TEST-ORDER-001", trade);

        OrderStatus status = tradeService.getOrderStatus("TEST-ORDER-001");
        assertEquals(OrderStatus.VALIDATED, status);
    }

    @Test
    void testGetOrderStatus_AllStatuses() throws Exception {
        Field mapField = TradeService.class.getDeclaredField("concurrentTradeStatusMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, com.klear.model.trade.Trade> map =
            (Map<String, com.klear.model.trade.Trade>) mapField.get(tradeService);

        // Test each status
        for (OrderStatus expectedStatus : OrderStatus.values()) {
            String orderId = "ORDER-" + expectedStatus.name();
            com.klear.model.trade.Trade trade = new com.klear.model.trade.Trade(
                orderId, testOrder, expectedStatus);
            map.put(orderId, trade);

            OrderStatus actualStatus = tradeService.getOrderStatus(orderId);
            assertEquals(expectedStatus, actualStatus);
        }
    }

    @Test
    void testConcurrentAccess() throws Exception {
        Field mapField = TradeService.class.getDeclaredField("concurrentTradeStatusMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, com.klear.model.trade.Trade> map =
            (Map<String, com.klear.model.trade.Trade>) mapField.get(tradeService);

        // Add multiple trades concurrently
        int numTrades = 100;
        Thread[] threads = new Thread[numTrades];

        for (int i = 0; i < numTrades; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                com.klear.model.trade.Trade trade = new com.klear.model.trade.Trade(
                    "CONCURRENT-" + index, testOrder, OrderStatus.VALIDATED);
                map.put("CONCURRENT-" + index, trade);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all trades were added
        assertEquals(numTrades, map.size());
        for (int i = 0; i < numTrades; i++) {
            assertEquals(OrderStatus.VALIDATED, tradeService.getOrderStatus("CONCURRENT-" + i));
        }
    }
}