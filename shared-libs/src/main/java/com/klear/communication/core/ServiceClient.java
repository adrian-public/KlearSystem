package com.klear.communication.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.trade.Trade;
import com.klear.services.TradeServiceCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import jakarta.annotation.PreDestroy;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.klear.communication.core.ServiceClientMessageTypes.ON_RECEIVE;
import static com.klear.communication.core.ServiceClientMessageTypes.SEND;

@Component
public abstract class ServiceClient implements ServiceClientInterface, ServiceClientCallback {

    private static final Logger log = LoggerFactory.getLogger(ServiceClient.class);

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

    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ServiceClient-subscriber");
        t.setDaemon(false);
        return t;
    });

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TradeServiceCallbackHandler tradeServiceCallbackHandler;

    public ServiceClient() {
        log.debug("ServiceClient created");
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
        subscriberExecutor.submit(() -> {
            try {
                jedisSub.subscribe(subscriber, this.retChannelName);
            } catch (Exception e) {
                log.error("Error in ServiceClient subscriber", e);
            }
        });
        log.info("ServiceClient initialized, listening on channel: {}", retChannelName);
    }

    @PreDestroy
    public void close() {
        log.info("Closing ServiceClient");
        if (subscriber != null && subscriber.isSubscribed()) {
            subscriber.unsubscribe();
        }
        subscriberExecutor.shutdown();
        try {
            if (!subscriberExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                subscriberExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            subscriberExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (jedisPubSend != null) {
            jedisPubSend.close();
        }
        if (jedisPubOnReceive != null) {
            jedisPubOnReceive.close();
        }
        if (jedisSub != null) {
            jedisSub.close();
        }
        log.info("ServiceClient closed");
    }

    @Override
    public void send(Trade trade) {
        try {
            ServiceClientMessage serviceClientMessage = new ServiceClientMessage(
                    SEND, this.retChannelName, (Object) trade);
            String message = objectMapper.writeValueAsString(serviceClientMessage);
            log.debug("ServiceClient sending: orderId={}", trade.getOrderId());
            jedisPubSend.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for trade: {}", trade.getOrderId(), e);
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
            log.error("Failed to parse received message", e);
            throw new RuntimeException(e);
        }
    }

    public void setTradeServiceCallbackHandler(TradeServiceCallbackHandler tradeServiceCallbackHandler) {
        this.tradeServiceCallbackHandler = tradeServiceCallbackHandler;
    }
}