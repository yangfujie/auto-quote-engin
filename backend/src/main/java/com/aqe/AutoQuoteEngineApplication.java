// AutoQuoteEngineApplication.java
package com.aqe;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableRabbit
public class AutoQuoteEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoQuoteEngineApplication.class, args);
    }
}