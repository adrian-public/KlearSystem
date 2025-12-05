package com.klear.communication.core;

import com.klear.model.trade.Trade;

public interface ServiceClientInterface {
    void send(Trade trade);
}
