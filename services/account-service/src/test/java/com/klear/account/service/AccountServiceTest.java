package com.klear.account.service;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService accountService;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();

        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testProcessTrade_ValidOrder() throws Exception {
        Trade trade = new Trade("ORDER-001", testOrder, OrderStatus.UNKNOWN);

        // Use reflection to call protected method
        Method processTrade = AccountService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(accountService, trade);

        assertEquals(OrderStatus.VALIDATED, result.getStatus());
        assertEquals("Account Validation Successful", result.getValidationMessage());
    }

    @Test
    void testProcessTrade_PreservesOrderData() throws Exception {
        Trade trade = new Trade("ORDER-002", testOrder, OrderStatus.UNKNOWN);

        Method processTrade = AccountService.class.getDeclaredMethod("processTrade", Trade.class);
        processTrade.setAccessible(true);
        Trade result = (Trade) processTrade.invoke(accountService, trade);

        // Verify order data is preserved
        assertEquals("ORDER-002", result.getOrderId());
        assertEquals(testOrder, result.getOrder());
        assertEquals("CLIENT123", result.getOrder().getClientId());
        assertEquals("AAPL", result.getOrder().getStockSymbol());
        assertEquals(100, result.getOrder().getQuantity());
        assertEquals(150.00, result.getOrder().getPrice(), 0.001);
    }

    @Test
    void testGetServiceName() {
        assertEquals("AccountService", accountService.getServiceName());
    }
}