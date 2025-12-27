package com.klear.communication.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceClientMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Order testOrder;
    private Trade testTrade;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setClientId("CLIENT123");
        testOrder.setStockSymbol("AAPL");
        testOrder.setQuantity(100);
        testOrder.setPrice(150.00);

        testTrade = new Trade("ORDER-001", testOrder, OrderStatus.UNKNOWN);
    }

    @Test
    void testConstructor() {
        // Use Object constructor (not Trade-specific one which has a bug)
        ServiceClientMessage message = new ServiceClientMessage(
            ServiceClientMessageTypes.SEND,
            "return_channel_123",
            (Object) testTrade
        );

        assertEquals(ServiceClientMessageTypes.SEND, message.getType());
        assertEquals("return_channel_123", message.getReturnChannel());
        assertNotNull(message.getPayload());
    }

    @Test
    void testDefaultConstructor() {
        ServiceClientMessage message = new ServiceClientMessage();
        assertNull(message.getType());
        assertNull(message.getReturnChannel());
        assertNull(message.getPayload());
    }

    @Test
    void testSetters() {
        ServiceClientMessage message = new ServiceClientMessage();

        message.setType(ServiceClientMessageTypes.ON_RECEIVE);
        message.setReturnChannel("test_channel");
        message.setPayload("test payload");

        assertEquals(ServiceClientMessageTypes.ON_RECEIVE, message.getType());
        assertEquals("test_channel", message.getReturnChannel());
        assertEquals("test payload", message.getPayload());
    }

    @Test
    void testJsonSerialization_SendType() throws Exception {
        ServiceClientMessage message = new ServiceClientMessage(
            ServiceClientMessageTypes.SEND,
            "channel_abc",
            (Object) testTrade
        );

        String json = objectMapper.writeValueAsString(message);

        assertTrue(json.contains("SEND"));
        assertTrue(json.contains("channel_abc"));
        assertTrue(json.contains("ORDER-001"));
    }

    @Test
    void testJsonSerialization_OnReceiveType() throws Exception {
        ServiceClientMessage message = new ServiceClientMessage(
            ServiceClientMessageTypes.ON_RECEIVE,
            "channel_xyz",
            (Object) testTrade
        );

        String json = objectMapper.writeValueAsString(message);

        assertTrue(json.contains("ON_RECEIVE"));
        assertTrue(json.contains("channel_xyz"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
            {
                "type": "SEND",
                "returnChannel": "test_return_channel",
                "payload": {"key": "value"}
            }
            """;

        ServiceClientMessage message = objectMapper.readValue(json, ServiceClientMessage.class);

        assertEquals(ServiceClientMessageTypes.SEND, message.getType());
        assertEquals("test_return_channel", message.getReturnChannel());
        assertNotNull(message.getPayload());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        ServiceClientMessage original = new ServiceClientMessage(
            ServiceClientMessageTypes.SEND,
            "roundtrip_channel",
            (Object) testTrade
        );

        String json = objectMapper.writeValueAsString(original);
        ServiceClientMessage deserialized = objectMapper.readValue(json, ServiceClientMessage.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getReturnChannel(), deserialized.getReturnChannel());
        assertNotNull(deserialized.getPayload());
    }

    @Test
    void testAllMessageTypes() {
        for (ServiceClientMessageTypes type : ServiceClientMessageTypes.values()) {
            ServiceClientMessage message = new ServiceClientMessage(type, "channel", "payload");
            assertEquals(type, message.getType());
        }
    }
}