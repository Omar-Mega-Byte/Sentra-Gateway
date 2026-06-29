package com.omar.sentra.payment.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core application beans shared across payment-service components.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Provides UTC time for service-owned timestamps and retention checks.
     *
     * @return system UTC clock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
