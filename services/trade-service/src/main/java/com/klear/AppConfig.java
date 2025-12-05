package com.klear;

import com.klear.communication.client.AccountServiceClient;
import com.klear.communication.client.ClearingServiceClient;
import com.klear.communication.client.ExecutionServiceClient;
import com.klear.communication.client.SettlementServiceClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AppConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AccountServiceClient AccountServiceClient() {
        return new AccountServiceClient();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ExecutionServiceClient ExecutionServiceClient() {
        return new ExecutionServiceClient();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ClearingServiceClient ClearingServiceClient() {
        return new ClearingServiceClient();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SettlementServiceClient SettlementServiceClient() {
        return new SettlementServiceClient();
    }
}
