package com.klear;

import com.klear.trade.service.client.TradeServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TradeServiceClient tradeServiceClient() {
        return new TradeServiceClient();
    }
}
