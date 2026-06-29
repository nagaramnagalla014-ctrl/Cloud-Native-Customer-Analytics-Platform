package com.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CustomerAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerAnalyticsApplication.class, args);
    }
}
