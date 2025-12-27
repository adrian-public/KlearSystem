package com.klear.exception;

/**
 * Base exception for all KLEAR system exceptions.
 */
public class KlearException extends RuntimeException {

    private final String orderId;

    public KlearException(String message) {
        super(message);
        this.orderId = null;
    }

    public KlearException(String message, String orderId) {
        super(message);
        this.orderId = orderId;
    }

    public KlearException(String message, Throwable cause) {
        super(message, cause);
        this.orderId = null;
    }

    public KlearException(String message, String orderId, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}