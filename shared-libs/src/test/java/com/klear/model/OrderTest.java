package com.klear.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.order.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGettersAndSetters() {
        Order order = new Order();
        order.setClientId("CLIENT123");
        order.setStockSymbol("AAPL");
        order.setQuantity(100);
        order.setPrice(150.50);

        assertEquals("CLIENT123", order.getClientId());
        assertEquals("AAPL", order.getStockSymbol());
        assertEquals(100, order.getQuantity());
        assertEquals(150.50, order.getPrice(), 0.001);
    }

    @Test
    void testJsonSerialization() throws Exception {
        Order order = new Order();
        order.setClientId("CLIENT456");
        order.setStockSymbol("MSFT");
        order.setQuantity(50);
        order.setPrice(300.00);

        String json = objectMapper.writeValueAsString(order);

        assertTrue(json.contains("CLIENT456"));
        assertTrue(json.contains("MSFT"));
        assertTrue(json.contains("50"));
        assertTrue(json.contains("300"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
            {
                "clientId": "CLIENT789",
                "stockSymbol": "GOOG",
                "quantity": 25,
                "price": 175.25
            }
            """;

        Order order = objectMapper.readValue(json, Order.class);

        assertEquals("CLIENT789", order.getClientId());
        assertEquals("GOOG", order.getStockSymbol());
        assertEquals(25, order.getQuantity());
        assertEquals(175.25, order.getPrice(), 0.001);
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        Order original = new Order();
        original.setClientId("ROUNDTRIP");
        original.setStockSymbol("TSLA");
        original.setQuantity(200);
        original.setPrice(250.75);

        String json = objectMapper.writeValueAsString(original);
        Order deserialized = objectMapper.readValue(json, Order.class);

        assertEquals(original.getClientId(), deserialized.getClientId());
        assertEquals(original.getStockSymbol(), deserialized.getStockSymbol());
        assertEquals(original.getQuantity(), deserialized.getQuantity());
        assertEquals(original.getPrice(), deserialized.getPrice(), 0.001);
    }
}