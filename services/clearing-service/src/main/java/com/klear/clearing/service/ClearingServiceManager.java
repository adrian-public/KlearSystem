package com.klear.clearing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClearingServiceManager {

    @Autowired
    private ClearingService clearingService;

    @Autowired
    public ClearingServiceManager(ClearingService clearingService) {
        this.clearingService = clearingService;
        new Thread(this.clearingService).start();
    }
}
