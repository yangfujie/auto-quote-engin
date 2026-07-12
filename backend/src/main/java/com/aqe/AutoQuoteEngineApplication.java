// AutoQuoteEngineApplication.java
package com.aqe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoQuoteEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoQuoteEngineApplication.class, args);
    }
}