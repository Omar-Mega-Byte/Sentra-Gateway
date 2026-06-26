package com.sentra.notification.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides time sources used by server-controlled timestamps.
 */
@Configuration
public class ClockConfig {

    /**
     * Returns the UTC clock used for response timestamps and preference
     * updates.
     *
     * @return UTC system clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
