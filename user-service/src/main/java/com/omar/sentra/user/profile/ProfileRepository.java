package com.omar.sentra.user.profile;

import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Repository contract required by the profile service.
 */
public interface ProfileRepository {

    Optional<UserProfile> findById(UUID id);

    Optional<UserProfile> findBySubject(String subject);

    /**
     * Atomically updates a profile when the expected version is current.
     *
     * @param id profile ID
     * @param expectedVersion caller's expected version
     * @param update update function applied under the repository lock
     * @return stored updated profile
     */
    UserProfile update(UUID id, long expectedVersion, UnaryOperator<UserProfile> update);

    /**
     * Resets deterministic in-memory state for isolated verification.
     */
    void reset();
}
