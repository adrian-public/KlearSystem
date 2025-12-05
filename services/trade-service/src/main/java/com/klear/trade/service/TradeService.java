package com.klear.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.communication.client.AccountServiceClient;
import com.klear.communication.client.ClearingServiceClient;
import com.klear.communication.client.ExecutionServiceClient;
import com.klear.communication.client.SettlementServiceClient;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.response.SettlementResponse;
import com.klear.model.trade.Trade;
import com.klear.services.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService
        implements TradeServiceClientInterface, TradeServiceCallbackHandler, TradeServiceClientCallback {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private ExecutionServiceClient executionServiceClient;

    @Autowired
    private ClearingServiceClient clearingServiceClient;

    @Autowired
    private SettlementServiceClient settlementServiceClient;

    private final Map<String, Trade> concurrentTradeStatusMap = new ConcurrentHashMap<>();

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${redis_ip}")
    private String ipAddress;

    @Value("${redis_port}")
    private int port;

    @Value("${trade_service_channel_name}")
    private String channelName;


    private Jedis jedisPub = null;
    private Jedis jedisSub = null;
    private JedisPubSub subscriber = null;
    private String outChannelName;

    public TradeService() {}

    @PostConstruct
    public void init() {
        this.accountServiceClient = applicationContext.getBean(AccountServiceClient.class);
        this.accountServiceClient.setTradeServiceCallbackHandler(this);

        this.executionServiceClient = applicationContext.getBean(ExecutionServiceClient.class);
        this.executionServiceClient.setTradeServiceCallbackHandler(this);

        this.executionServiceClient = applicationContext.getBean(ExecutionServiceClient.class);
        this.executionServiceClient.setTradeServiceCallbackHandler(this);

        this.clearingServiceClient = applicationContext.getBean(ClearingServiceClient.class);
        this.clearingServiceClient.setTradeServiceCallbackHandler(this);

        this.settlementServiceClient = applicationContext.getBean(SettlementServiceClient.class);
        this.settlementServiceClient.setTradeServiceCallbackHandler(this);

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

    /**
     * Retrieves the current status of an order.
     *
     * @param orderId The order ID.
     * @return The current status of the order.
     */
    public OrderStatus getOrderStatus(String orderId) {
        Trade trade = concurrentTradeStatusMap.getOrDefault(orderId, null);
        if (trade == null) {
            return OrderStatus.UNKNOWN;
        }
        System.out.println("getOrderStatus: " + orderId + " Status: " + trade.getStatus());
        return trade.getStatus();
    }

    /**
     * Accepts and validates a new trade order from the client.
     *
     * @param order The trade order details.
     * @return The unique order ID for tracking purposes.
     */
    public String submitOrder(Order order) {
        // Generate a unique order ID
        String orderId = UUID.randomUUID().toString();
        Trade trade = new Trade(orderId, order, OrderStatus.UNKNOWN);

        // Add the trade to the map
        concurrentTradeStatusMap.put(orderId, trade);

        // Route the order for account validation (e.g., margin requirements, credit limits)
        // accountService.validateAccount(trade);
        accountServiceClient.send(trade);

        System.out.println("submitOrder:    " + orderId);
        return orderId;
    }

    /**
     * Callback handler used to notify the TradeService that the Settlement service has Settled a Trade.
     *
     * @param settlementResponse The settlement response.
     */
    public void onSettlement(SettlementResponse settlementResponse) {
        String orderId = settlementResponse.getOrderId();
        Trade trade = concurrentTradeStatusMap.get(orderId);
        trade.setSettlementMessage(settlementResponse.getMessage());

        // Update order status
        trade.setStatus(OrderStatus.SETTLED);
    }

    @Override
    public void callbackHandler(String channel, String message) {
        try {
            TradeServiceClientMessage tradeServiceClientMessage = objectMapper.readValue(message, TradeServiceClientMessage.class);
            switch (tradeServiceClientMessage.getType()) {
                case ORDER_SUBMIT: {
                    String jsonString = objectMapper.writeValueAsString(tradeServiceClientMessage.getPayload());
                    Order order = objectMapper.readValue(jsonString, Order.class);
                    String returnChannel = tradeServiceClientMessage.getReturnChannel();
                    String orderId = submitOrder(order);
                    tradeServiceClientMessage.setReturnChannel("");
                    tradeServiceClientMessage.setPayload(orderId);
                    String response = objectMapper.writeValueAsString(tradeServiceClientMessage);
                    jedisPub.publish(returnChannel, response);
                }
                break;
                case ORDER_STATUS: {
                    String jsonString = objectMapper.writeValueAsString(tradeServiceClientMessage.getPayload());
                    String orderId = objectMapper.readValue(jsonString, String.class);
                    String returnChannel = tradeServiceClientMessage.getReturnChannel();
                    OrderStatus orderStatus = getOrderStatus(orderId);
                    tradeServiceClientMessage.setReturnChannel("");
                    tradeServiceClientMessage.setPayload(orderStatus);
                    String response = objectMapper.writeValueAsString(tradeServiceClientMessage);
                    jedisPub.publish(returnChannel, response);
                }
                break;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback handler used to notify the TradeService that the AccountService has validated a Trade.
     *
     * @param trade - to manage the lifecycle of the Trade.
     */
    @Override
    public void onValidation(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case VALIDATED: {
                masterTrade.setStatus(OrderStatus.VALIDATED);
                masterTrade.setValidationMessage(trade.getValidationMessage());
                System.out.println("Validated: " + trade.getOrderId());  // Output the JSON string
                executionServiceClient.send(masterTrade);
            }
        }
    }

    /**
     * Callback handler used to notify the TradeService that the ExecutionService has executed a Trade.
     *
     * @param trade - to manage the lifecycle of the Trade.
     */
    @Override
    public void onExecution(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case EXECUTED: {
                masterTrade.setStatus(OrderStatus.EXECUTED);
                masterTrade.setExecutedTimestamp(trade.getExecutedTimestamp());
                masterTrade.setExecutedPrice(trade.getExecutedPrice());
                System.out.println("Executed:  " + trade.getOrderId());  // Output the JSON string
                clearingServiceClient.send(masterTrade);
            }
        }
    }

    /**
     * Callback handler used to notify the TradeService that the ClearingService has cleared a Trade.
     *
     * @param trade - to manage the lifecycle of the Trade.
     */
    @Override
    public void onClearing(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case CLEARED: {
                masterTrade.setStatus(OrderStatus.CLEARED);
                masterTrade.setNettedAmount(trade.getNettedAmount());
                masterTrade.setClearingMessage(trade.getClearingMessage());
                System.out.println("Cleared:   " + trade.getOrderId());  // Output the JSON string
                settlementServiceClient.send(masterTrade);
            }
        }
    }

    /**
     * Callback handler used to notify the TradeService that the SettlementService has Settled a Trade.
     *
     * @param trade - to manage the lifecycle of the Trade.
     */
    @Override
    public void onSettlement(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case SETTLED: {
                masterTrade.setStatus(OrderStatus.SETTLED);
                masterTrade.setSettlementMessage(trade.getSettlementMessage());
                System.out.println("Settled:   " + trade.getOrderId());  // Output the JSON string
            }
        }
    }
}
