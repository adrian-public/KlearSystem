package com.klear.communication.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.model.queue.QueueItem;
import com.klear.model.queue.QueueItemTypes;
import com.klear.model.trade.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.klear.communication.core.ServiceClientMessageTypes.ON_RECEIVE;
import static com.klear.communication.core.ServiceClientMessageTypes.SEND;

/**
 * Abstract base class for all downstream services (Account, Execution, Clearing, Settlement).
 * Provides common Redis pub/sub infrastructure, queue processing, and lifecycle management.
 */
public abstract class BaseService implements ServiceClientCallback, Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final BlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>();

    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, getServiceName() + "-subscriber");
        t.setDaemon(false);
        return t;
    });

    protected String outChannelName;
    protected Jedis jedisPub;
    protected Jedis jedisSub;
    protected JedisPubSub subscriber;

    /**
     * Returns the service name for logging and thread naming.
     */
    protected abstract String getServiceName();

    /**
     * Returns the channel name from configuration.
     */
    protected abstract String getChannelName();

    /**
     * Returns the Redis host from configuration.
     */
    protected abstract String getRedisHost();

    /**
     * Returns the Redis port from configuration.
     */
    protected abstract int getRedisPort();

    /**
     * Returns the queue item type this service processes.
     */
    protected abstract QueueItemTypes getQueueItemType();

    /**
     * Process the trade and return the modified trade with updated status.
     * Subclasses implement their specific business logic here.
     */
    protected abstract Trade processTrade(Trade trade);

    /**
     * Initialize Redis connections and start the subscriber thread.
     * Call this from @PostConstruct in subclasses.
     */
    protected void initializeRedis() {
        this.outChannelName = getChannelName() + "_OUT";

        if (this.jedisPub == null) {
            this.jedisPub = new Jedis(getRedisHost(), getRedisPort());
        }
        if (this.jedisSub == null) {
            this.jedisSub = new Jedis(getRedisHost(), getRedisPort());
        }

        subscriber = new JedisPubSubAsync(this);
        subscriberExecutor.submit(() -> {
            try {
                jedisSub.subscribe(subscriber, this.outChannelName);
            } catch (Exception e) {
                log.error("Error in Redis subscriber for {}", getServiceName(), e);
            }
        });

        log.info("{} initialized, listening on channel: {}", getServiceName(), outChannelName);
    }

    @Override
    public void run() {
        log.info("{} processing loop started", getServiceName());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                QueueItem queueItem = queue.take();
                processQueueItem(queueItem);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("{} processing loop interrupted", getServiceName());
        }
    }

    private void processQueueItem(QueueItem queueItem) {
        if (queueItem.getType() != getQueueItemType()) {
            log.warn("Unexpected queue item type: {} (expected {})",
                    queueItem.getType(), getQueueItemType());
            return;
        }

        try {
            ServiceClientMessage serviceClientMessage = (ServiceClientMessage) queueItem.getItem();
            if (serviceClientMessage.getType() != SEND) {
                return;
            }

            String jsonString = objectMapper.writeValueAsString(serviceClientMessage.getPayload());
            Trade trade = objectMapper.readValue(jsonString, Trade.class);

            // Delegate to subclass for business logic
            Trade processedTrade = processTrade(trade);

            // Send response back
            serviceClientMessage.setPayload(processedTrade);
            serviceClientMessage.setType(ON_RECEIVE);
            String returnChannel = serviceClientMessage.getReturnChannel();
            String response = objectMapper.writeValueAsString(serviceClientMessage);

            log.info("{}: {} orderId={}", getServiceName(),
                    processedTrade.getStatus(), processedTrade.getOrderId());
            jedisPub.publish(returnChannel, response);

        } catch (JsonProcessingException e) {
            log.error("JSON processing error in {}", getServiceName(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReceive(String channel, String message) {
        try {
            ServiceClientMessage serviceClientMessage = objectMapper.readValue(message, ServiceClientMessage.class);
            QueueItem queueItem = new QueueItem(getQueueItemType(), serviceClientMessage);
            queue.put(queueItem);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse message in {}", getServiceName(), e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while queuing message in {}", getServiceName());
        }
    }

    /**
     * Shutdown the service gracefully.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {}", getServiceName());
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
        log.info("{} shutdown complete", getServiceName());
    }
}