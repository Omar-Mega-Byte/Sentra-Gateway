package com.omar.sentra.order.config;

import java.time.Clock;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadFeature;

/**
 * Core application infrastructure.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Supplies UTC time to domain services, idempotency, and error responses.
     *
     * @return system UTC clock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Rejects duplicate JSON object keys instead of silently accepting a
     * last-value-wins interpretation.
     *
     * @return Jackson builder customizer
     */
    @Bean
    JsonMapperBuilderCustomizer strictDuplicateJsonKeys() {
        return builder -> builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
    }
}
