package com.loblaw.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpringBoot starter
 */
@SpringBootApplication
@EnableScheduling
public class FalconMetricsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FalconMetricsApplication.class, args);
	}
}
