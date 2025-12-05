package com.klear.communication.client;

import com.klear.communication.core.ServiceClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceClient extends ServiceClient {

    @Value("${account_service_channel_name}")
    private String channelName;

    public AccountServiceClient() {}

    @PostConstruct
    public void setup(){
        super.channelName = channelName;
        super.init();
    }
}
