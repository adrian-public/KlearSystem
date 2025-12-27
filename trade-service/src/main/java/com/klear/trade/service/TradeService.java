package com.klear.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.communication.client.AccountServiceClient;
import com.klear.communication.client.ClearingServiceClient;
import com.klear.communication.client.ExecutionServiceClient;
import com.klear.communication.client.SettlementServiceClient;
import com.klear.communication.core.JedisPubSubAsync;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.model.response.SettlementResponse;
import com.klear.model.trade.Trade;
import com.klear.communication.core.ServiceClientCallback;
import com.klear.services.TradeServiceCallbackHandler;
import com.klear.services.TradeServiceClientInterface;
import com.klear.services.TradeServiceClientMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class TradeService
        implements TradeServiceClientInterface, TradeServiceCallbackHandler, ServiceClientCallback {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TradeService-subscriber");
        t.setDaemon(false);
        return t;
    });

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
        subscriberExecutor.submit(() -> {
            try {
                jedisSub.subscribe(subscriber, this.outChannelName);
            } catch (Exception e) {
                log.error("Error in TradeService subscriber", e);
            }
        });

        log.info("TradeService initialized, listening on channel: {}", outChannelName);
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
        log.debug("getOrderStatus: orderId={} status={}", orderId, trade.getStatus());
        return trade.getStatus();
    }

    /**
     * Accepts and validates a new trade order from the client.
     *
     * @param order The trade order details.
     * @return The unique order ID for tracking purposes.
     */
    public String submitOrder(Order order) {
        String orderId = UUID.randomUUID().toString();
        Trade trade = new Trade(orderId, order, OrderStatus.UNKNOWN);

        concurrentTradeStatusMap.put(orderId, trade);
        accountServiceClient.send(trade);

        log.info("Order submitted: orderId={}", orderId);
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
        trade.setStatus(OrderStatus.SETTLED);
    }

    @Override
    public void onReceive(String channel, String message) {
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
            log.error("Failed to process callback message", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onValidation(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case VALIDATED: {
                masterTrade.setStatus(OrderStatus.VALIDATED);
                masterTrade.setValidationMessage(trade.getValidationMessage());
                log.info("Trade validated: orderId={}", trade.getOrderId());
                executionServiceClient.send(masterTrade);
            }
        }
    }

    @Override
    public void onExecution(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case EXECUTED: {
                masterTrade.setStatus(OrderStatus.EXECUTED);
                masterTrade.setExecutedTimestamp(trade.getExecutedTimestamp());
                masterTrade.setExecutedPrice(trade.getExecutedPrice());
                log.info("Trade executed: orderId={}", trade.getOrderId());
                clearingServiceClient.send(masterTrade);
            }
        }
    }

    @Override
    public void onClearing(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case CLEARED: {
                masterTrade.setStatus(OrderStatus.CLEARED);
                masterTrade.setNettedAmount(trade.getNettedAmount());
                masterTrade.setClearingMessage(trade.getClearingMessage());
                log.info("Trade cleared: orderId={}", trade.getOrderId());
                settlementServiceClient.send(masterTrade);
            }
        }
    }

    @Override
    public void onSettlement(Trade trade) {
        String orderId = trade.getOrderId();
        Trade masterTrade = concurrentTradeStatusMap.get(orderId);
        switch (trade.getStatus()) {
            case SETTLED: {
                masterTrade.setStatus(OrderStatus.SETTLED);
                masterTrade.setSettlementMessage(trade.getSettlementMessage());
                log.info("Trade settled: orderId={}", trade.getOrderId());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TradeService");
        if (subscriber != null && subscriber.isSubscribed()) {
            subscriber.unsubscribe();
        }
        subscriberExecutor.shutdown();
        try {
            if (!subscriberExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                subscriberExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            subscriberExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (jedisPub != null) {
            jedisPub.close();
        }
        if (jedisSub != null) {
            jedisSub.close();
        }
        log.info("TradeService shutdown complete");
    }
}