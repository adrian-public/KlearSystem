package com.klear.account.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceManager {

    @Autowired
    private AccountService accountService;

    @Autowired
    public AccountServiceManager(AccountService accountService) {
        this.accountService = accountService;
        new Thread(this.accountService).start();
    }
}
