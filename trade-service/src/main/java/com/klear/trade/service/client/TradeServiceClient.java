package com.klear.trade.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.communication.core.ServiceClientCallback;
import com.klear.communication.core.ServiceClientMessageTypes;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.services.JedisPubSubSync;
import com.klear.services.TradeServiceClientInterface;
import com.klear.services.TradeServiceClientMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import jakarta.annotation.PreDestroy;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TradeServiceClient implements TradeServiceClientInterface, ServiceClientCallback {

    private static final Logger log = LoggerFactory.getLogger(TradeServiceClient.class);

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${trade_service_channel_name}")
    private String channelName;

    private Jedis jedisPubSubmit = null;
    private Jedis jedisPubStatus = null;
    private Jedis jedisSub = null;
    private JedisPubSub subscriber = null;
    private String retChannelName;
    private String outChannelName;

    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TradeServiceClient-subscriber");
        t.setDaemon(false);
        return t;
    });

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Object mutex = new Object();
    private String orderId;
    public OrderStatus orderStatus;

    public TradeServiceClient() {
        log.debug("TradeServiceClient created");
    }

    @PostConstruct
    public void init() {
        this.outChannelName = this.channelName + "_OUT";
        this.retChannelName = this.channelName + "_RET_" + UUID.randomUUID();
        if (this.jedisPubSubmit == null) {
            this.jedisPubSubmit = new Jedis(ipAddress, port);
        }
        if (this.jedisPubStatus == null) {
            this.jedisPubStatus = new Jedis(ipAddress, port);
        }
        if (this.jedisSub == null) {
            this.jedisSub = new Jedis(ipAddress, port);
        }
        subscriber = new JedisPubSubSync(this, mutex);
        subscriberExecutor.submit(() -> {
            try {
                jedisSub.subscribe(subscriber, this.retChannelName);
            } catch (Exception e) {
                log.error("Error in TradeServiceClient subscriber", e);
            }
        });

        log.info("TradeServiceClient initialized, listening on channel: {}", retChannelName);
    }

    @Override
    public String submitOrder(Order order) {
        try {
            TradeServiceClientMessage tradeServiceMessage = new TradeServiceClientMessage(
                    ServiceClientMessageTypes.ORDER_SUBMIT, this.retChannelName, order);
            String message = objectMapper.writeValueAsString(tradeServiceMessage);
            log.debug("Submitting order: {}", order);
            jedisPubSubmit.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order submission", e);
        }

        synchronized (mutex) {
            try {
                this.mutex.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return orderId;
    }

    @Override
    public OrderStatus getOrderStatus(String orderId) {
        try {
            TradeServiceClientMessage tradeServiceMessage = new TradeServiceClientMessage(
                    ServiceClientMessageTypes.ORDER_STATUS, this.retChannelName, orderId);
            String message = objectMapper.writeValueAsString(tradeServiceMessage);
            log.debug("Getting order status: orderId={}", orderId);
            jedisPubStatus.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order status request", e);
        }

        synchronized (mutex) {
            try {
                this.mutex.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return orderStatus;
    }

    @Override
    public void onReceive(String channel, String message) {
        try {
            TradeServiceClientMessage tradeServiceClientMessage = objectMapper.readValue(message, TradeServiceClientMessage.class);
            switch (tradeServiceClientMessage.getType()) {
                case ORDER_SUBMIT: {
                    String jsonString = objectMapper.writeValueAsString(tradeServiceClientMessage.getPayload());
                    this.orderId = jsonString;
                }
                break;
                case ORDER_STATUS: {
                    String jsonString = objectMapper.writeValueAsString(tradeServiceClientMessage.getPayload());
                    String status = objectMapper.readValue(jsonString, String.class);
                    this.orderStatus = OrderStatus.valueOf(status);
                }
                break;
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse callback message", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TradeServiceClient");
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
        if (jedisPubSubmit != null) {
            jedisPubSubmit.close();
        }
        if (jedisPubStatus != null) {
            jedisPubStatus.close();
        }
        if (jedisSub != null) {
            jedisSub.close();
        }
        log.info("TradeServiceClient shutdown complete");
    }
}