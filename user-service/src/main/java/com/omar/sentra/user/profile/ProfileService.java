package com.omar.sentra.user.profile;

import static com.omar.sentra.user.common.error.ServiceErrors.profileNotFound;

import com.omar.sentra.user.common.error.UserServiceException;
import com.omar.sentra.user.observability.ProfileMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implements active-profile lookup, safe DTO mapping, and optimistic updates.
 */
@Service
public class ProfileService {
    private final ProfileRepository repository;
    private final ProfileValidator validator;
    private final ProfileMetrics metrics;
    private final Clock clock;

    public ProfileService(
            ProfileRepository repository,
            ProfileValidator validator,
            ProfileMetrics metrics,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Returns the redacted public profile for an active profile ID.
     *
     * @param id profile ID
     * @return public response
     */
    public PublicProfileResponse publicProfile(UUID id) {
        UserProfile profile = repository.findById(id).orElse(null);
        if (!active(profile)) {
            metrics.lookup("public-profile-read", "not-found");
            throw profileNotFound();
        }
        metrics.lookup("public-profile-read", "success");
        return new PublicProfileResponse(
                profile.id(),
                profile.displayName(),
                profile.bio(),
                profile.avatarUrl());
    }

    /**
     * Resolves an active profile exclusively from the trusted subject.
     *
     * @param subject trusted identity subject
     * @return safe current-profile response
     */
    public CurrentProfileResponse currentProfile(String subject) {
        UserProfile profile = activeBySubject(subject);
        metrics.lookup("profile-read", "success");
        return current(profile);
    }

    /**
     * Validates and atomically updates the trusted subject's active profile.
     *
     * @param subject trusted identity subject
     * @param request merge-style patch
     * @return safe current-profile response
     */
    public CurrentProfileResponse updateCurrentProfile(
            String subject,
            ProfilePatchRequest request) {
        UserProfile current = activeBySubject(subject);
        ValidatedProfilePatch patch = validator.validate(request, current);
        if (unchanged(current, patch)) {
            metrics.update("no-op");
            return current(current);
        }
        UserProfile updated;
        try {
            updated = repository.update(
                    current.id(),
                    patch.expectedVersion(),
                    stored -> new UserProfile(
                            stored.id(),
                            stored.subject(),
                            patch.displayName(),
                            patch.bio(),
                            patch.avatarUrl(),
                            patch.email(),
                            patch.locale(),
                            patch.timezone(),
                            stored.status(),
                            stored.version() + 1,
                            stored.createdAt(),
                            Instant.now(clock)));
        } catch (UserServiceException exception) {
            if ("USR_VERSION_CONFLICT".equals(exception.code())) {
                metrics.update("conflict");
            }
            throw exception;
        }
        metrics.update("success");
        return current(updated);
    }

    private UserProfile activeBySubject(String subject) {
        UserProfile profile = repository.findBySubject(subject).orElse(null);
        if (!active(profile)) {
            metrics.lookup("profile-read", "not-found");
            throw profileNotFound();
        }
        return profile;
    }

    private static boolean active(UserProfile profile) {
        return profile != null && profile.status() == ProfileStatus.ACTIVE;
    }

    private static boolean unchanged(UserProfile current, ValidatedProfilePatch patch) {
        return Objects.equals(current.displayName(), patch.displayName())
                && Objects.equals(current.bio(), patch.bio())
                && Objects.equals(current.avatarUrl(), patch.avatarUrl())
                && Objects.equals(current.email(), patch.email())
                && Objects.equals(current.locale(), patch.locale())
                && Objects.equals(current.timezone(), patch.timezone());
    }

    private static CurrentProfileResponse current(UserProfile profile) {
        return new CurrentProfileResponse(
                profile.id(),
                profile.displayName(),
                profile.bio(),
                profile.avatarUrl(),
                profile.email(),
                profile.locale(),
                profile.timezone(),
                profile.version(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
