package com.omar.sentra.user;

import com.omar.sentra.user.config.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Starts the Sentra user-profile service.
 */
@SpringBootApplication
@EnableConfigurationProperties(UserServiceProperties.class)
public class UserServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
