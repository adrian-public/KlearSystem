package com.klear.services;


import com.klear.model.trade.Trade;

public interface TradeServiceCallbackHandler {
    void onValidation(Trade tade);
    void onExecution(Trade trade);
    void onClearing(Trade trade);
    void onSettlement(Trade trade);
}
