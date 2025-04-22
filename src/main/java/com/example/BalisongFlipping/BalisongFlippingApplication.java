package com.example.BalisongFlipping;

import com.example.BalisongFlipping.repositories.AccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class BalisongFlippingApplication {

	Logger log = LoggerFactory.getLogger(BalisongFlippingApplication.class); 

	public static void main(String[] args) {
		SpringApplication.run(BalisongFlippingApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(AccountRepository accountRepository, MongoTemplate mongoTemplate) {

		log.info("INFO: Application successfully booted!");
		
		return args -> {
			accountRepository.deleteAll();
		};
	}
}
