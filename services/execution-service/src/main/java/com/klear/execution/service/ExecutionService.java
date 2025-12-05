package com.klear.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.communication.core.JedisPubSubAsync;
import com.klear.communication.core.ServiceClientCallback;
import com.klear.communication.core.ServiceClientMessage;
import com.klear.model.order.Order;
import com.klear.model.queue.QueueItem;
import com.klear.model.response.ExecutionResponse;
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
import static com.klear.model.order.OrderStatus.EXECUTED;
import static com.klear.model.queue.QueueItemTypes.EXECUTION;

@Service
public class ExecutionService implements ServiceClientCallback, Runnable {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${execution_service_channel_name}")
    private String channelName;

    private String outChannelName;
    private Jedis jedisPub = null;
    private Jedis jedisSub = null;
    private JedisPubSub subscriber = null;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>();

    public ExecutionService() {}

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
            case EXECUTION: {
                try {
                    ServiceClientMessage serviceClientMessage = (ServiceClientMessage) queueItem.getItem();
                    if (serviceClientMessage.getType() == SEND) {
                        String jsonString = objectMapper.writeValueAsString(serviceClientMessage.getPayload());
                        Trade trade = objectMapper.readValue(jsonString, Trade.class);

                        // Simulate trade matching and execution logic on an exchange
                        String orderId = trade.getOrderId();
                        double executedPrice = trade.getOrder().getPrice(); // Simulated price (e.g., real logic would check liquidity)
                        long timestamp = System.currentTimeMillis(); // Simulate execution time
                        ExecutionResponse executionResponse = new ExecutionResponse(orderId, executedPrice, timestamp);
                        trade.setExecutedPrice(executionResponse.getExecutedPrice());
                        trade.setExecutedTimestamp(timestamp);
                        trade.setStatus(EXECUTED);

                        serviceClientMessage.setPayload(trade);
                        serviceClientMessage.setType(ON_RECEIVE);
                        String returnChannel = serviceClientMessage.getReturnChannel();
                        String response = objectMapper.writeValueAsString(serviceClientMessage);
                        System.out.println("Executed:  " + trade.getOrderId());  // Output the JSON string
                        jedisPub.publish(returnChannel, response);
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
            executeTrade(serviceClientMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeTrade(ServiceClientMessage serviceClientMessage) {
        try {
            QueueItem queueItem = new QueueItem(EXECUTION, serviceClientMessage);
            queue.put(queueItem);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
