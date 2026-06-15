package com.omar.sentra.user.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core application infrastructure beans.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Supplies UTC time to domain services and error responses.
     *
     * @return system UTC clock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
