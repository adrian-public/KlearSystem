package com.klear.clearing.service;

import com.klear.communication.core.BaseService;
import com.klear.model.order.OrderStatus;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.response.ClearingResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClearingService extends BaseService {

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

        ClearingResponse clearingResponse = new ClearingResponse(
                trade.getOrderId(), nettedAmount, "Clearing Successful");
        trade.setNettedAmount(clearingResponse.getNettedAmount());
        trade.setClearingMessage(clearingResponse.getMessage());
        trade.setStatus(OrderStatus.CLEARED);

        return trade;
    }
}