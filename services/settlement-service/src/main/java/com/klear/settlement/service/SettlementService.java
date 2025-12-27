package com.klear.settlement.service;

import com.klear.communication.core.BaseService;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.klear.model.order.OrderStatus.SETTLED;

@Service
public class SettlementService extends BaseService {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${settlement_service_channel_name}")
    private String channelName;

    @PostConstruct
    public void init() {
        initializeRedis();
    }

    @Override
    protected String getServiceName() {
        return "SettlementService";
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
        return QueueItemTypes.SETTLEMENT;
    }

    @Override
    protected Trade processTrade(Trade trade) {
        // Simulate settlement logic
        trade.setSettlementMessage("Settlement Successful");
        trade.setStatus(SETTLED);

        return trade;
    }
}