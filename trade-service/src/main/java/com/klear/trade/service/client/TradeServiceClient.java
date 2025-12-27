package com.klear.trade.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.services.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // Set the bean scope to prototype
public class TradeServiceClient implements TradeServiceClientInterface, TradeServiceClientCallback {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Object mutex = new Object();
    private String orderId;
    public OrderStatus orderStatus;

    public TradeServiceClient() {
        System.out.println("TradeServiceClient created.");
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
        new Thread(() -> {
            try {
                jedisSub.subscribe(subscriber, this.retChannelName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public String submitOrder(Order order) {
        try {
            TradeServiceClientMessage tradeServiceMessage = new TradeServiceClientMessage(
                    TradeServiceClientMessageTypes.ORDER_SUBMIT, this.retChannelName, order);
            String message = objectMapper.writeValueAsString(tradeServiceMessage);
            System.out.println("SubmitOrder:    " + message);  // Output the JSON string
            jedisPubSubmit.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        synchronized (mutex) {
            try {
                this.mutex.wait(); // Wait until another thread notifies and releases the lock
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Handle interruption
            }
        }

        return orderId;
    }

    @Override
    public OrderStatus getOrderStatus(String orderId) {
        try {
            TradeServiceClientMessage tradeServiceMessage = new TradeServiceClientMessage(
                    TradeServiceClientMessageTypes.ORDER_STATUS, this.retChannelName, orderId);
            String message = objectMapper.writeValueAsString(tradeServiceMessage);
            System.out.println(message);  // Output the JSON string
            System.out.println("getOrderStatus: " + message);  // Output the JSON string
            jedisPubStatus.publish(this.outChannelName, message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        synchronized (mutex) {
            try {
                this.mutex.wait(); // Wait until another thread notifies and releases the lock
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Handle interruption
            }
        }

        return orderStatus;
    }

    @Override
    public void callbackHandler(String channel, String message) {
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
            throw new RuntimeException(e);
        }
    }
}
