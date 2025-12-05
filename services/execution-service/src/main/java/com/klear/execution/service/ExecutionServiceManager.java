package com.klear.execution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExecutionServiceManager {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    public ExecutionServiceManager(ExecutionService executionService) {
        this.executionService = executionService;
        new Thread(this.executionService).start();
    }
}
