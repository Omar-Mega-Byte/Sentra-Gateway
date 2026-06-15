package com.omar.sentra.user.observability;

import com.omar.sentra.user.profile.ProfileRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness contributor for the required profile repository.
 */
@Component("profileRepositoryHealthIndicator")
public class ProfileRepositoryHealthIndicator implements HealthIndicator {
    private final ProfileRepository repository;

    public ProfileRepositoryHealthIndicator(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            repository.findBySubject("__sentra_health_probe__");
            return Health.up().withDetail("mode", "memory").build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("mode", "memory").build();
        }
    }
}
