package com.omar.sentra.payment;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Starts the Sentra payment service.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PaymentServiceProperties.class)
public class PaymentServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
