package com.fbposter.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TokenShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(TokenShopApplication.class, args);
	}

}
