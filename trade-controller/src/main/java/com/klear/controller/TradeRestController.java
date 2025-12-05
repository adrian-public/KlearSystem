package com.klear.controller;

import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.services.TradeServiceClientInterface;
import com.klear.trade.service.client.TradeServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeRestController {

    private final ApplicationContext applicationContext;
    private final TradeServiceClientInterface tradeServiceClientInterface;

    @Autowired
    public TradeRestController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.tradeServiceClientInterface = applicationContext.getBean(TradeServiceClient.class);
    }

    /**
     * API endpoint to submit a trade order.
     *
     * @param order The trade order details (client ID, stock symbol, quantity, and price).
     * @return Response with the unique order ID.
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submitOrder(@RequestBody Order order) {
        try {
            String orderId = tradeServiceClientInterface.submitOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body("Order submitted successfully. Order ID: " + orderId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order submission failed: " + e.getMessage());
        }
    }

    /**
     * API endpoint to check the status of an order.
     *
     * @param orderId The unique order ID.
     * @return Response with the current status of the order.
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<String> getOrderStatus(@PathVariable String orderId) {
        OrderStatus status = tradeServiceClientInterface.getOrderStatus(orderId);

        if (status == OrderStatus.UNKNOWN) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found. Order ID: " + orderId);
        }

        return ResponseEntity.ok("Order Status: " + status.name());
    }
}
