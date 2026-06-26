package com.omar.sentra.user.profile;

import com.omar.sentra.user.config.UserServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the documented profile repository implementation.
 */
@Configuration
public class ProfileConfiguration {

    /**
     * Creates the instance-local in-memory profile repository.
     *
     * @param properties user-service settings
     * @return profile repository
     */
    @Bean
    ProfileRepository profileRepository(UserServiceProperties properties) {
        return new InMemoryProfileRepository(properties);
    }
}
