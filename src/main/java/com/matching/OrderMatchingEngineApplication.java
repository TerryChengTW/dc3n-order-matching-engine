package com.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderMatchingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMatchingEngineApplication.class, args);
    }

}
