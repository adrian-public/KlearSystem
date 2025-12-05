package com.klear.communication.client;

import com.klear.communication.core.ServiceClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SettlementServiceClient extends ServiceClient {

    @Value("${settlement_service_channel_name}")
    private String channelName;

    public SettlementServiceClient() {}

    @PostConstruct
    public void setup(){
        super.channelName = channelName;
        super.init();
    }

}
