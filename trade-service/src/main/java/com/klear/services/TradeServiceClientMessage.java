package com.klear.services;

import com.klear.communication.core.ServiceClientMessageTypes;

/**
 * Message wrapper for communication with TradeService.
 * Uses the common ServiceClientMessageTypes enum.
 */
public class TradeServiceClientMessage {
    private ServiceClientMessageTypes type;
    private String returnChannel;
    private Object payload;

    public TradeServiceClientMessage() {
    }

    public TradeServiceClientMessage(ServiceClientMessageTypes type, String retChannelName, Object payload) {
        this.type = type;
        this.returnChannel = retChannelName;
        this.payload = payload;
    }

    public ServiceClientMessageTypes getType() {
        return type;
    }

    public void setType(ServiceClientMessageTypes type) {
        this.type = type;
    }

    public String getReturnChannel() {
        return returnChannel;
    }

    public void setReturnChannel(String returnChannel) {
        this.returnChannel = returnChannel;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}