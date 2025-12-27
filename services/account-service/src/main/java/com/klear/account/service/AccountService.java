package com.klear.account.service;

import com.klear.communication.core.BaseService;
import com.klear.model.order.Order;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.response.AccountResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.klear.model.order.OrderStatus.VALIDATED;

@Service
public class AccountService extends BaseService {

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${account_service_channel_name}")
    private String channelName;

    @PostConstruct
    public void init() {
        initializeRedis();
    }

    @Override
    protected String getServiceName() {
        return "AccountService";
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
        return QueueItemTypes.VALIDATION;
    }

    @Override
    protected Trade processTrade(Trade trade) {
        Order order = trade.getOrder();

        // Simulate Account validation
        if (checkMargin(order) && checkCreditLimits(order)) {
            AccountResponse accountResponse = new AccountResponse(
                    trade.getOrderId(), "Account Validation Successful");
            trade.setValidationMessage(accountResponse.getMessage());
            trade.setStatus(VALIDATED);
        }

        return trade;
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