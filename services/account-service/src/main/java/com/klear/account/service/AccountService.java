package com.klear.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.communication.core.JedisPubSubAsync;
import com.klear.communication.core.ServiceClientCallback;
import com.klear.communication.core.ServiceClientMessage;
import com.klear.model.order.Order;
import com.klear.model.queue.QueueItem;
import com.klear.model.response.AccountResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.klear.communication.core.ServiceClientMessageTypes.ON_RECEIVE;
import static com.klear.communication.core.ServiceClientMessageTypes.SEND;
import static com.klear.model.order.OrderStatus.VALIDATED;
import static com.klear.model.queue.QueueItemTypes.VALIDATION;

@Service
public class AccountService implements ServiceClientCallback, Runnable {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${account_service_channel_name}")
    private String channelName;

    private String outChannelName;
    private Jedis jedisPub = null;
    private Jedis jedisSub = null;
    private JedisPubSub subscriber = null;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>();

    public AccountService() {}

    @PostConstruct
    public void init() {
        this.outChannelName = this.channelName + "_OUT";
        if (this.jedisPub == null) {
            this.jedisPub = new Jedis(ipAddress, port);
        }
        if (this.jedisSub == null) {
            this.jedisSub = new Jedis(ipAddress, port);
        }
        subscriber = new JedisPubSubAsync(this);
        new Thread(() -> {
            try {
                jedisSub.subscribe(subscriber, this.outChannelName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    @Override
    public void run() {
        try {
            while (true) {
                // This is a blocking take()
                QueueItem queueItem = queue.take();
                processQueueItem(queueItem);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processQueueItem(QueueItem queueItem) {
        switch (queueItem.getType()) {
            case VALIDATION: {
                try {
                    ServiceClientMessage serviceClientMessage = (ServiceClientMessage) queueItem.getItem();
                    if (serviceClientMessage.getType() == SEND) {
                        String jsonString = objectMapper.writeValueAsString(serviceClientMessage.getPayload());
                        Trade trade = objectMapper.readValue(jsonString, Trade.class);
                        Order order = trade.getOrder();

                        // Simulate Account validation
                        if (checkMargin(order) && checkCreditLimits(order)) {
                            AccountResponse accountResponse = new AccountResponse(trade.getOrderId(), "Account Validation Successful");
                            trade.setValidationMessage(accountResponse.getMessage());
                            trade.setStatus(VALIDATED);
                            serviceClientMessage.setPayload(trade);
                            serviceClientMessage.setType(ON_RECEIVE);
                            String returnChannel = serviceClientMessage.getReturnChannel();
                            String response = objectMapper.writeValueAsString(serviceClientMessage);
                            System.out.println("Validated: " + trade.getOrderId());  // Output the JSON string
                            jedisPub.publish(returnChannel, response);
                        }
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        }
    }

    @Override
    public void onReceive(String channel, String message) {
        try {
            ServiceClientMessage serviceClientMessage = objectMapper.readValue(message, ServiceClientMessage.class);
            validateAccount(serviceClientMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void validateAccount(ServiceClientMessage serviceClientMessage) {
        try {
            QueueItem queueItem = new QueueItem(VALIDATION, serviceClientMessage);
            queue.put(queueItem);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checkMargin(Order order) {
        // Simulate Account validation
        return true;
    }

    private boolean checkCreditLimits(Order order) {
        // Simulate Account validation
        return true;
    }
}
