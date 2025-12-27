package com.klear.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klear.TradeRestControllerApplication;
import com.klear.model.order.Order;
import com.klear.model.order.OrderStatus;
import com.klear.services.TradeServiceClientInterface;
import com.klear.trade.service.client.TradeServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TradeRestController.class)
@ContextConfiguration(classes = TradeRestControllerApplication.class)
class TradeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TradeServiceClient tradeServiceClient;

    @Test
    void testSubmitOrder_Success() throws Exception {
        Order order = createTestOrder();
        when(tradeServiceClient.submitOrder(any(Order.class))).thenReturn("ORDER-123");

        mockMvc.perform(post("/api/trades/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ORDER-123")));

        verify(tradeServiceClient).submitOrder(any(Order.class));
    }

    @Test
    void testSubmitOrder_Failure() throws Exception {
        Order order = createTestOrder();
        when(tradeServiceClient.submitOrder(any(Order.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        mockMvc.perform(post("/api/trades/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("failed")));
    }

    @Test
    void testGetOrderStatus_Found() throws Exception {
        when(tradeServiceClient.getOrderStatus("ORDER-123")).thenReturn(OrderStatus.VALIDATED);

        mockMvc.perform(get("/api/trades/ORDER-123/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATED")));

        verify(tradeServiceClient).getOrderStatus("ORDER-123");
    }

    @Test
    void testGetOrderStatus_NotFound() throws Exception {
        when(tradeServiceClient.getOrderStatus("UNKNOWN-ORDER")).thenReturn(OrderStatus.UNKNOWN);

        mockMvc.perform(get("/api/trades/UNKNOWN-ORDER/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not found")));
    }

    @Test
    void testGetOrderStatus_AllStatuses() throws Exception {
        OrderStatus[] statuses = {
                OrderStatus.VALIDATED,
                OrderStatus.EXECUTED,
                OrderStatus.CLEARED,
                OrderStatus.SETTLED
        };

        for (OrderStatus status : statuses) {
            when(tradeServiceClient.getOrderStatus("ORDER-" + status.name())).thenReturn(status);

            mockMvc.perform(get("/api/trades/ORDER-" + status.name() + "/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(status.name())));
        }
    }

    private Order createTestOrder() {
        Order order = new Order();
        order.setClientId("CLIENT123");
        order.setStockSymbol("AAPL");
        order.setQuantity(100);
        order.setPrice(150.00);
        return order;
    }
}