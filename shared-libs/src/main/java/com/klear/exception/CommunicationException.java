package com.klear.exception;

/**
 * Exception thrown when inter-service communication fails.
 */
public class CommunicationException extends KlearException {

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(String message, String orderId) {
        super(message, orderId);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(String message, String orderId, Throwable cause) {
        super(message, orderId, cause);
    }
}