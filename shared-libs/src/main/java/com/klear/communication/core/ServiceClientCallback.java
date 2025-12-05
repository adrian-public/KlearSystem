package com.klear.communication.core;


public interface ServiceClientCallback {
    void onReceive(String channel, String message);
}
