package com.omar.sentra.order.order;

import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.observability.OrderMetrics;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Selects the documented order repository implementation.
 */
@Configuration
public class OrderConfiguration {

    /**
     * Creates the instance-local in-memory repository.
     *
     * @param properties order-service settings
     * @param clock service clock
     * @param metrics bounded order metrics
     * @return order repository
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "sentra.order.repository",
            name = "mode",
            havingValue = "memory")
    OrderRepository orderRepository(
            OrderServiceProperties properties,
            Clock clock,
            OrderMetrics metrics) {
        return new InMemoryOrderRepository(properties, clock, metrics);
    }

    /**
     * Creates the PostgreSQL-backed repository.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "sentra.order.repository",
            name = "mode",
            havingValue = "postgres",
            matchIfMissing = true)
    OrderRepository jdbcOrderRepository(
            OrderServiceProperties properties,
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            OrderMetrics metrics) {
        return new JdbcOrderRepository(properties, jdbc, transactions, metrics);
    }
}
