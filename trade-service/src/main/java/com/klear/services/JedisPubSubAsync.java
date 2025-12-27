package com.klear.services;

import redis.clients.jedis.JedisPubSub;

public class JedisPubSubAsync extends JedisPubSub {
    private final TradeServiceClientCallback tradeServiceClientCallback;
    public JedisPubSubAsync(TradeServiceClientCallback tradeServiceClientCallback) {
        this.tradeServiceClientCallback = tradeServiceClientCallback;
    }

    @Override
    public void onMessage(String channel, String message) {
        // This method will be called when a message is received
        //System.out.println("Received message from channel " + channel + ": " + message);
        this.tradeServiceClientCallback.callbackHandler(channel, message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        //System.out.println("Subscribed to channel: " + channel);
    }
}
