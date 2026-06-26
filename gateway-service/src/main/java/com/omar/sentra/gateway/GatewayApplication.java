package com.omar.sentra.gateway;

import com.omar.sentra.gateway.config.SentraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Starts the Sentra reactive API gateway.
 */
@SpringBootApplication
@EnableConfigurationProperties(SentraProperties.class)
public class GatewayApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
