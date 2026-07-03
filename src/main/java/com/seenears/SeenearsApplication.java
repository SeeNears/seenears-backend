package com.seenears;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SeenearsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeenearsApplication.class, args);
	}

}
