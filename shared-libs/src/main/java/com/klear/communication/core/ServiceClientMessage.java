package com.klear.communication.core;

import com.klear.model.trade.Trade;

public class ServiceClientMessage {
    private ServiceClientMessageTypes type;
    private String returnChannel;
    private Object payload;

    public ServiceClientMessage() {
    }

    public ServiceClientMessage(ServiceClientMessageTypes send, String retChannelName, Trade trade) {
    }

    public ServiceClientMessage(ServiceClientMessageTypes type, String retChannelName, Object payload) {
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
