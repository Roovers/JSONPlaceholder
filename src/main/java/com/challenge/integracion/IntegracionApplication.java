package com.challenge.integracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IntegracionApplication {
	public static void main(String[] args) {
		SpringApplication.run(IntegracionApplication.class, args);
	}
}
