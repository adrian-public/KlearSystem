package com.klear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.klear.controller", "com.klear.trade.service.client"})
public class TradeRestControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeRestControllerApplication.class, args);
	}

}
