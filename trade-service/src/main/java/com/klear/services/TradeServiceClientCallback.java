package com.klear.services;

public interface TradeServiceClientCallback {
    void callbackHandler(String channel, String message);
}
