package com.klear.services;

import com.klear.communication.core.ServiceClientCallback;
import redis.clients.jedis.JedisPubSub;

/**
 * Synchronous JedisPubSub implementation that notifies a mutex after processing.
 * Used for request-response patterns where the caller waits for a response.
 */
public class JedisPubSubSync extends JedisPubSub {
    private final ServiceClientCallback serviceClientCallback;
    private final Object mutex;

    public JedisPubSubSync(ServiceClientCallback serviceClientCallback, Object mutex) {
        this.serviceClientCallback = serviceClientCallback;
        this.mutex = mutex;
    }

    @Override
    public void onMessage(String channel, String message) {
        synchronized (mutex) {
            this.serviceClientCallback.onReceive(channel, message);
            mutex.notify();
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        // Subscription confirmed
    }
}