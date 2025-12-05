package com.klear.services;

public class TradeServiceClientMessage {
    private TradeServiceClientMessageTypes type;
    private String returnChannel;
    private Object payload;

    public TradeServiceClientMessage() {
    }

    public TradeServiceClientMessage(TradeServiceClientMessageTypes type, String retChannelName, Object payload) {
        this.type = type;
        this.returnChannel = retChannelName;
        this.payload = payload;
    }

    public TradeServiceClientMessageTypes getType() {
        return type;
    }

    public void setType(TradeServiceClientMessageTypes type) {
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
