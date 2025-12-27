package com.klear.clearing.service;

import com.klear.communication.core.BaseService;
import com.klear.model.order.OrderStatus;

import static com.klear.model.order.OrderStatus.FAILED;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.response.ClearingResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClearingService extends BaseService {

    private static final double MAX_NETTED_AMOUNT = 1_000_000.0;

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${clearing_service_channel_name}")
    private String channelName;

    @PostConstruct
    public void init() {
        initializeRedis();
    }

    @Override
    protected String getServiceName() {
        return "ClearingService";
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
        return QueueItemTypes.CLEARING;
    }

    @Override
    protected Trade processTrade(Trade trade) {
        // Simulate clearing with a CCP
        double nettedAmount = trade.getOrder().getQuantity() * trade.getExecutedPrice();

        // Check risk limit
        if (nettedAmount > MAX_NETTED_AMOUNT) {
            trade.setStatus(FAILED);
            trade.setFailureStage("CLEARING");
            trade.setFailureReason("Risk limit exceeded: netted amount " + nettedAmount + " exceeds max " + MAX_NETTED_AMOUNT);
            trade.setClearingMessage("Clearing failed: Risk limit exceeded");
            return trade;
        }

        ClearingResponse clearingResponse = new ClearingResponse(
                trade.getOrderId(), nettedAmount, "Clearing Successful");
        trade.setNettedAmount(clearingResponse.getNettedAmount());
        trade.setClearingMessage(clearingResponse.getMessage());
        trade.setStatus(OrderStatus.CLEARED);

        return trade;
    }
}