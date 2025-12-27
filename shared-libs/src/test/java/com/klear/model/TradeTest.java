package com.klear.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);
    }

    @Test
    void testConstructor() {
        Trade trade = new Trade("ORDER-001", testOrder, OrderStatus.UNKNOWN);

        assertEquals("ORDER-001", trade.getOrderId());
        assertEquals(testOrder, trade.getOrder());
        assertEquals(OrderStatus.UNKNOWN, trade.getStatus());
    }

    @Test
    void testStatusTransitions() {
        Trade trade = new Trade("ORDER-002", testOrder, OrderStatus.UNKNOWN);

        // UNKNOWN -> VALIDATED
        trade.setStatus(OrderStatus.VALIDATED);
        assertEquals(OrderStatus.VALIDATED, trade.getStatus());

        // VALIDATED -> EXECUTED
        trade.setStatus(OrderStatus.EXECUTED);
        assertEquals(OrderStatus.EXECUTED, trade.getStatus());

        // EXECUTED -> CLEARED
        trade.setStatus(OrderStatus.CLEARED);
        assertEquals(OrderStatus.CLEARED, trade.getStatus());

        // CLEARED -> SETTLED
        trade.setStatus(OrderStatus.SETTLED);
        assertEquals(OrderStatus.SETTLED, trade.getStatus());
    }

    @Test
    void testValidationMessage() {
        Trade trade = new Trade("ORDER-003", testOrder, OrderStatus.UNKNOWN);

        trade.setValidationMessage("Account validated successfully");
        assertEquals("Account validated successfully", trade.getValidationMessage());
    }

    @Test
    void testExecutionFields() {
        Trade trade = new Trade("ORDER-004", testOrder, OrderStatus.UNKNOWN);

        trade.setExecutedPrice(151.50);
        trade.setExecutedTimestamp(1234567890L);

        assertEquals(151.50, trade.getExecutedPrice(), 0.001);
        assertEquals(1234567890L, trade.getExecutedTimestamp());
    }

    @Test
    void testClearingFields() {
        Trade trade = new Trade("ORDER-005", testOrder, OrderStatus.UNKNOWN);

        trade.setNettedAmount(15150.00);
        trade.setClearingMessage("Clearing successful");

        assertEquals(15150.00, trade.getNettedAmount(), 0.001);
        assertEquals("Clearing successful", trade.getClearingMessage());
    }

    @Test
    void testSettlementMessage() {
        Trade trade = new Trade("ORDER-006", testOrder, OrderStatus.UNKNOWN);

        trade.setSettlementMessage("Settlement complete");
        assertEquals("Settlement complete", trade.getSettlementMessage());
    }

    @Test
    void testJsonSerialization() throws Exception {
        Trade trade = new Trade("ORDER-007", testOrder, OrderStatus.VALIDATED);
        trade.setValidationMessage("Valid");

        String json = objectMapper.writeValueAsString(trade);

        assertTrue(json.contains("ORDER-007"));
        assertTrue(json.contains("VALIDATED"));
        assertTrue(json.contains("Valid"));
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        Trade original = new Trade("ORDER-008", testOrder, OrderStatus.EXECUTED);
        original.setValidationMessage("Validated");
        original.setExecutedPrice(152.00);
        original.setExecutedTimestamp(9876543210L);

        String json = objectMapper.writeValueAsString(original);
        Trade deserialized = objectMapper.readValue(json, Trade.class);

        assertEquals(original.getOrderId(), deserialized.getOrderId());
        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getValidationMessage(), deserialized.getValidationMessage());
        assertEquals(original.getExecutedPrice(), deserialized.getExecutedPrice(), 0.001);
        assertEquals(original.getExecutedTimestamp(), deserialized.getExecutedTimestamp());
    }

    @Test
    void testDefaultMessageValues() {
        Trade trade = new Trade("ORDER-009", testOrder, OrderStatus.UNKNOWN);

        // Default values should be empty strings
        assertEquals("", trade.getValidationMessage());
        assertEquals("", trade.getClearingMessage());
        assertEquals("", trade.getSettlementMessage());
    }

    @Test
    void testFailureFields() {
        Trade trade = new Trade("ORDER-010", testOrder, OrderStatus.FAILED);

        trade.setFailureReason("Position limit exceeded");
        trade.setFailureStage("VALIDATION");

        assertEquals("Position limit exceeded", trade.getFailureReason());
        assertEquals("VALIDATION", trade.getFailureStage());
    }

    @Test
    void testDefaultFailureFieldValues() {
        Trade trade = new Trade("ORDER-011", testOrder, OrderStatus.UNKNOWN);

        // Default values should be empty strings
        assertEquals("", trade.getFailureReason());
        assertEquals("", trade.getFailureStage());
    }

    @Test
    void testFailedStatusTransition() {
        Trade trade = new Trade("ORDER-012", testOrder, OrderStatus.UNKNOWN);

        // UNKNOWN -> FAILED (can fail at any stage)
        trade.setStatus(OrderStatus.FAILED);
        trade.setFailureStage("VALIDATION");
        trade.setFailureReason("Invalid client ID");

        assertEquals(OrderStatus.FAILED, trade.getStatus());
        assertEquals("VALIDATION", trade.getFailureStage());
        assertEquals("Invalid client ID", trade.getFailureReason());
    }

    @Test
    void testJsonSerializationWithFailure() throws Exception {
        Trade trade = new Trade("ORDER-013", testOrder, OrderStatus.FAILED);
        trade.setFailureReason("Risk limit exceeded");
        trade.setFailureStage("CLEARING");

        String json = objectMapper.writeValueAsString(trade);

        assertTrue(json.contains("ORDER-013"));
        assertTrue(json.contains("FAILED"));
        assertTrue(json.contains("Risk limit exceeded"));
        assertTrue(json.contains("CLEARING"));
    }

    @Test
    void testJsonRoundTripWithFailure() throws Exception {
        Trade original = new Trade("ORDER-014", testOrder, OrderStatus.FAILED);
        original.setFailureReason("Price exceeds maximum");
        original.setFailureStage("VALIDATION");
        original.setValidationMessage("Validation failed");

        String json = objectMapper.writeValueAsString(original);
        Trade deserialized = objectMapper.readValue(json, Trade.class);

        assertEquals(original.getOrderId(), deserialized.getOrderId());
        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getFailureReason(), deserialized.getFailureReason());
        assertEquals(original.getFailureStage(), deserialized.getFailureStage());
    }
}