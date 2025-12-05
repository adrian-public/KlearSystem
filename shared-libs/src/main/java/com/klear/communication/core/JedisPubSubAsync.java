package com.klear.communication.core;

import redis.clients.jedis.JedisPubSub;

public class JedisPubSubAsync extends JedisPubSub {
    private final ServiceClientCallback serviceClientCallback;
    public JedisPubSubAsync(ServiceClientCallback serviceClientCallback) {
        this.serviceClientCallback = serviceClientCallback;
    }

    @Override
    public void onMessage(String channel, String message) {
        // This method will be called when a message is received
        this.serviceClientCallback.onReceive(channel, message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        //System.out.println("Subscribed to channel: " + channel);
    }
}
