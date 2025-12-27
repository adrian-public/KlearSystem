package com.klear.model;

import com.klear.model.order.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testAllStatusValues() {
        OrderStatus[] statuses = OrderStatus.values();

        assertEquals(6, statuses.length);
        assertArrayEquals(
            new OrderStatus[]{
                OrderStatus.VALIDATED,
                OrderStatus.EXECUTED,
                OrderStatus.CLEARED,
                OrderStatus.SETTLED,
                OrderStatus.FAILED,
                OrderStatus.UNKNOWN
            },
            statuses
        );
    }

    @Test
    void testValueOf() {
        assertEquals(OrderStatus.UNKNOWN, OrderStatus.valueOf("UNKNOWN"));
        assertEquals(OrderStatus.VALIDATED, OrderStatus.valueOf("VALIDATED"));
        assertEquals(OrderStatus.EXECUTED, OrderStatus.valueOf("EXECUTED"));
        assertEquals(OrderStatus.CLEARED, OrderStatus.valueOf("CLEARED"));
        assertEquals(OrderStatus.SETTLED, OrderStatus.valueOf("SETTLED"));
        assertEquals(OrderStatus.FAILED, OrderStatus.valueOf("FAILED"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            OrderStatus.valueOf("INVALID_STATUS");
        });
    }

    @Test
    void testOrdinalOrder() {
        // Verify the lifecycle order (VALIDATED comes first in enum declaration)
        assertTrue(OrderStatus.VALIDATED.ordinal() < OrderStatus.EXECUTED.ordinal());
        assertTrue(OrderStatus.EXECUTED.ordinal() < OrderStatus.CLEARED.ordinal());
        assertTrue(OrderStatus.CLEARED.ordinal() < OrderStatus.SETTLED.ordinal());
    }

    @Test
    void testToString() {
        assertEquals("UNKNOWN", OrderStatus.UNKNOWN.toString());
        assertEquals("VALIDATED", OrderStatus.VALIDATED.toString());
        assertEquals("EXECUTED", OrderStatus.EXECUTED.toString());
        assertEquals("CLEARED", OrderStatus.CLEARED.toString());
        assertEquals("SETTLED", OrderStatus.SETTLED.toString());
        assertEquals("FAILED", OrderStatus.FAILED.toString());
    }

    @Test
    void testName() {
        assertEquals("UNKNOWN", OrderStatus.UNKNOWN.name());
        assertEquals("VALIDATED", OrderStatus.VALIDATED.name());
        assertEquals("EXECUTED", OrderStatus.EXECUTED.name());
        assertEquals("CLEARED", OrderStatus.CLEARED.name());
        assertEquals("SETTLED", OrderStatus.SETTLED.name());
        assertEquals("FAILED", OrderStatus.FAILED.name());
    }
}