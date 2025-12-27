package com.klear.exception;

/**
 * Exception thrown when account validation fails.
 */
public class ValidationException extends KlearException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, String orderId) {
        super(message, orderId);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(String message, String orderId, Throwable cause) {
        super(message, orderId, cause);
    }
}