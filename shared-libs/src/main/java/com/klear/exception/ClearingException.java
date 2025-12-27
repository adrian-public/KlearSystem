package com.klear.exception;

/**
 * Exception thrown when trade clearing fails.
 */
public class ClearingException extends KlearException {

    public ClearingException(String message) {
        super(message);
    }

    public ClearingException(String message, String orderId) {
        super(message, orderId);
    }

    public ClearingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClearingException(String message, String orderId, Throwable cause) {
        super(message, orderId, cause);
    }
}