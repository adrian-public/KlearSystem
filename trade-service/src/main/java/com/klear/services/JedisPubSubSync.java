package com.klear.services;

import redis.clients.jedis.JedisPubSub;

public class JedisPubSubSync extends JedisPubSub {
    private final TradeServiceClientCallback tradeServiceClientCallback;
    private final Object mutex;
    public JedisPubSubSync(TradeServiceClientCallback tradeServiceClientCallback, Object mutex) {
        this.tradeServiceClientCallback = tradeServiceClientCallback;
        this.mutex = mutex;
    }

    @Override
    public void onMessage(String channel, String message) {
        // This method will be called when a message is received
        // System.out.println("Received message from channel " + channel + ": " + message);
        synchronized (mutex) {
            this.tradeServiceClientCallback.callbackHandler(channel, message);
            mutex.notify();
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        //System.out.println("Subscribed to channel: " + channel);
    }
}
