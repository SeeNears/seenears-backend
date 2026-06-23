package com.seenears;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

@SpringBootTest
class SeenearsApplicationTests {

	@DynamicPropertySource
	static void jwtProperties(DynamicPropertyRegistry registry) {
		registry.add("jwt.secret", () -> UUID.randomUUID().toString());
	}

	@Test
	void contextLoads() {
	}

}
