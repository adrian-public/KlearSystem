package com.klear.exception;

/**
 * Exception thrown when trade execution fails.
 */
public class ExecutionException extends KlearException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, String orderId) {
        super(message, orderId);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionException(String message, String orderId, Throwable cause) {
        super(message, orderId, cause);
    }
}