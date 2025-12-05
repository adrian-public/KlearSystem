package com.klear.communication.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.trade.Trade;
import com.klear.services.TradeServiceCallbackHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

import static com.klear.communication.core.ServiceClientMessageTypes.ON_RECEIVE;
import static com.klear.communication.core.ServiceClientMessageTypes.SEND;

@Component
public abstract class ServiceClient implements ServiceClientInterface, ServiceClientCallback {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    protected String channelName = "";

    private Jedis jedisPubSend = null;
    private Jedis jedisPubOnReceive = null;
    private Jedis jedisSub = null;
    private JedisPubSub subscriber = null;
    private String retChannelName;
    private String outChannelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TradeServiceCallbackHandler tradeServiceCallbackHandler;

    public ServiceClient() {
        System.out.println("ServiceClient created.");
    }

    public void init() {
        this.outChannelName = this.channelName + "_OUT";
        this.retChannelName = this.channelName + "_RET_" + UUID.randomUUID();
        if (this.jedisPubSend == null) {
            this.jedisPubSend = new Jedis(ipAddress, port);
        }
        if (this.jedisPubOnReceive == null) {
            this.jedisPubOnReceive = new Jedis(ipAddress, port);
        }
        if (this.jedisSub == null) {
            this.jedisSub = new Jedis(ipAddress, port);
        }
        subscriber = new JedisPubSubAsync(this);
        new Thread(() -> {
            try {
                jedisSub.subscribe(subscriber, this.retChannelName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Close the Redis connection when done
    public void close() {
    }

    @Override
    public void send(Trade trade) {
        try {
            ServiceClientMessage serviceClientMessage = new ServiceClientMessage(
                    SEND, this.retChannelName, (Object) trade);
            String message = objectMapper.writeValueAsString(serviceClientMessage);
            System.out.println("ServiceClient: send: " + message);  // Output the JSON string
            jedisPubSend.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(String channel, String message) {
        try {
            ServiceClientMessage serviceClientMessage = objectMapper.readValue(message, ServiceClientMessage.class);
            if (serviceClientMessage.getType() == ON_RECEIVE) {
                String jsonString = objectMapper.writeValueAsString(serviceClientMessage.getPayload());
                Trade trade = objectMapper.readValue(jsonString, Trade.class);
                if (this.tradeServiceCallbackHandler != null) {

                    switch (trade.getStatus()) {
                        case VALIDATED: {
                            this.tradeServiceCallbackHandler.onValidation(trade);
                        }
                        break;
                        case EXECUTED: {
                            this.tradeServiceCallbackHandler.onExecution(trade);
                        }
                        break;
                        case CLEARED: {
                            this.tradeServiceCallbackHandler.onClearing(trade);
                        }
                        break;
                        case SETTLED: {
                            this.tradeServiceCallbackHandler.onSettlement(trade);
                        }
                        break;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTradeServiceCallbackHandler(TradeServiceCallbackHandler tradeServiceCallbackHandler) {
        this.tradeServiceCallbackHandler = tradeServiceCallbackHandler;
    }

}
