package com.klear.exception;

/**
 * Exception thrown when trade settlement fails.
 */
public class SettlementException extends KlearException {

    public SettlementException(String message) {
        super(message);
    }

    public SettlementException(String message, String orderId) {
        super(message, orderId);
    }

    public SettlementException(String message, Throwable cause) {
        super(message, cause);
    }

    public SettlementException(String message, String orderId, Throwable cause) {
        super(message, orderId, cause);
    }
}