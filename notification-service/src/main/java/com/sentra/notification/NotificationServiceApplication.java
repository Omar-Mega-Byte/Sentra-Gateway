package com.sentra.notification;

import com.sentra.notification.config.NotificationServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bootstraps the internal Sentra notification demonstration service.
 *
 * <p>The gateway owns external authentication and resilience policy; this
 * application owns deterministic downstream state, trusted-context validation,
 * local/test fault simulation, and redacted service telemetry.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(NotificationServiceProperties.class)
public class NotificationServiceApplication {

    /**
     * Starts the notification service.
     *
     * @param args command line arguments passed by Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
