package com.klear.settlement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettlementServiceManager {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    public SettlementServiceManager(SettlementService settlementService) {
        this.settlementService = settlementService;
        new Thread(this.settlementService).start();
    }
}
