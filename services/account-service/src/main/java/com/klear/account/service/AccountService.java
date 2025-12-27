package com.klear.account.service;

import com.klear.communication.core.BaseService;
import com.klear.model.order.Order;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.response.AccountResponse;
import com.klear.model.trade.Trade;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.klear.model.order.OrderStatus.FAILED;
import static com.klear.model.order.OrderStatus.VALIDATED;

@Service
public class AccountService extends BaseService {

    private static final int MAX_QUANTITY = 10000;
    private static final double MAX_PRICE = 10000.0;

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

        // Validate client ID
        if (order.getClientId() == null || order.getClientId().isEmpty()) {
            trade.setStatus(FAILED);
            trade.setFailureStage("VALIDATION");
            trade.setFailureReason("Invalid client ID");
            trade.setValidationMessage("Validation failed: Invalid client ID");
            return trade;
        }

        // Check position limits
        if (!checkPositionLimits(order)) {
            trade.setStatus(FAILED);
            trade.setFailureStage("VALIDATION");
            trade.setFailureReason("Position limit exceeded (max " + MAX_QUANTITY + ")");
            trade.setValidationMessage("Validation failed: Position limit exceeded");
            return trade;
        }

        // Check price limits
        if (!checkPriceLimits(order)) {
            trade.setStatus(FAILED);
            trade.setFailureStage("VALIDATION");
            trade.setFailureReason("Price exceeds maximum (max " + MAX_PRICE + ")");
            trade.setValidationMessage("Validation failed: Price exceeds maximum");
            return trade;
        }

        // All validations passed
        AccountResponse accountResponse = new AccountResponse(
                trade.getOrderId(), "Account Validation Successful");
        trade.setValidationMessage(accountResponse.getMessage());
        trade.setStatus(VALIDATED);

        return trade;
    }

    private boolean checkPositionLimits(Order order) {
        return order.getQuantity() <= MAX_QUANTITY;
    }

    private boolean checkPriceLimits(Order order) {
        return order.getPrice() <= MAX_PRICE;
    }
}