package com.klear.execution.service;

import com.klear.communication.core.BaseService;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.response.ExecutionResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.klear.model.order.OrderStatus.EXECUTED;
import static com.klear.model.order.OrderStatus.FAILED;

@Service
public class ExecutionService extends BaseService {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${execution_service_channel_name}")
    private String channelName;

    @PostConstruct
    public void init() {
        initializeRedis();
    }

    @Override
    protected String getServiceName() {
        return "ExecutionService";
    }

    @Override
    protected String getChannelName() {
        return channelName;
    }

    @Override
    protected String getRedisHost() {
        return ipAddress;
    }

    @Override
    protected int getRedisPort() {
        return port;
    }

    @Override
    protected QueueItemTypes getQueueItemType() {
        return QueueItemTypes.EXECUTION;
    }

    @Override
    protected Trade processTrade(Trade trade) {
        // Validate price
        if (trade.getOrder().getPrice() <= 0) {
            trade.setStatus(FAILED);
            trade.setFailureStage("EXECUTION");
            trade.setFailureReason("Invalid price: must be greater than 0");
            return trade;
        }

        // Validate quantity
        if (trade.getOrder().getQuantity() <= 0) {
            trade.setStatus(FAILED);
            trade.setFailureStage("EXECUTION");
            trade.setFailureReason("Invalid quantity: must be greater than 0");
            return trade;
        }

        // Simulate trade matching and execution logic on an exchange
        String orderId = trade.getOrderId();
        double executedPrice = trade.getOrder().getPrice(); // Simulated price
        long timestamp = System.currentTimeMillis();

        ExecutionResponse executionResponse = new ExecutionResponse(orderId, executedPrice, timestamp);
        trade.setExecutedPrice(executionResponse.getExecutedPrice());
        trade.setExecutedTimestamp(timestamp);
        trade.setStatus(EXECUTED);

        return trade;
    }
}