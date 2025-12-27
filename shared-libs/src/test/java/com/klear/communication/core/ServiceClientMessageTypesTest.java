package com.klear.communication.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceClientMessageTypesTest {

    @Test
    void testAllMessageTypes() {
        ServiceClientMessageTypes[] types = ServiceClientMessageTypes.values();

        assertEquals(5, types.length);
    }

    @Test
    void testValueOf() {
        assertEquals(ServiceClientMessageTypes.ORDER_SUBMIT, ServiceClientMessageTypes.valueOf("ORDER_SUBMIT"));
        assertEquals(ServiceClientMessageTypes.ORDER_STATUS, ServiceClientMessageTypes.valueOf("ORDER_STATUS"));
        assertEquals(ServiceClientMessageTypes.SEND, ServiceClientMessageTypes.valueOf("SEND"));
        assertEquals(ServiceClientMessageTypes.ON_RECEIVE, ServiceClientMessageTypes.valueOf("ON_RECEIVE"));
        assertEquals(ServiceClientMessageTypes.UNKNOWN, ServiceClientMessageTypes.valueOf("UNKNOWN"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            ServiceClientMessageTypes.valueOf("INVALID_TYPE");
        });
    }

    @Test
    void testName() {
        assertEquals("ORDER_SUBMIT", ServiceClientMessageTypes.ORDER_SUBMIT.name());
        assertEquals("ORDER_STATUS", ServiceClientMessageTypes.ORDER_STATUS.name());
        assertEquals("SEND", ServiceClientMessageTypes.SEND.name());
        assertEquals("ON_RECEIVE", ServiceClientMessageTypes.ON_RECEIVE.name());
        assertEquals("UNKNOWN", ServiceClientMessageTypes.UNKNOWN.name());
    }
}