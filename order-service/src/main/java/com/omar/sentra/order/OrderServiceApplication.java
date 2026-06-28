package com.omar.sentra.order;

import com.omar.sentra.order.config.OrderServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Starts the Sentra order service.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OrderServiceProperties.class)
public class OrderServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
