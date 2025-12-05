package com.klear.model.order;

public enum OrderStatus {
    VALIDATED, // Order is submitted but not yet executed
    EXECUTED,          // Order is executed on the exchange
    CLEARED,           // Order has cleared with the clearing house (CCP)
    SETTLED,           // Order has been settled with the central securities depository (CSD)
    FAILED,            // Order failed at any stage (execution, clearing, or settlement)
    UNKNOWN
}
