package com.omar.sentra.user.profile;

import static com.omar.sentra.user.common.error.ServiceErrors.emailConflict;
import static com.omar.sentra.user.common.error.ServiceErrors.profileNotFound;
import static com.omar.sentra.user.common.error.ServiceErrors.versionConflict;

import com.omar.sentra.user.config.UserServiceProperties;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Synchronized in-memory repository for the documented demonstration baseline.
 */
public class InMemoryProfileRepository implements ProfileRepository {
    private final Object monitor = new Object();
    private final boolean seedEnabled;
    private final boolean emailUnique;
    private final Map<UUID, UserProfile> profiles = new HashMap<>();
    private final Map<String, UUID> subjects = new HashMap<>();
    private final Map<String, UUID> emails = new HashMap<>();

    public InMemoryProfileRepository(UserServiceProperties properties) {
        this.seedEnabled = properties.repository().seedEnabled();
        this.emailUnique = properties.repository().emailUnique();
        reset();
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        synchronized (monitor) {
            return Optional.ofNullable(profiles.get(id));
        }
    }

    @Override
    public Optional<UserProfile> findBySubject(String subject) {
        synchronized (monitor) {
            UUID id = subjects.get(subject);
            return id == null ? Optional.empty() : Optional.ofNullable(profiles.get(id));
        }
    }

    @Override
    public UserProfile update(
            UUID id,
            long expectedVersion,
            UnaryOperator<UserProfile> update) {
        synchronized (monitor) {
            UserProfile current = profiles.get(id);
            if (current == null) {
                throw profileNotFound();
            }
            if (current.version() != expectedVersion) {
                throw versionConflict();
            }
            UserProfile candidate = update.apply(current);
            if (emailUnique) {
                String normalizedEmail = normalizeEmail(candidate.email());
                UUID owner = emails.get(normalizedEmail);
                if (owner != null && !owner.equals(id)) {
                    throw emailConflict();
                }
                emails.remove(normalizeEmail(current.email()));
                emails.put(normalizedEmail, id);
            }
            profiles.put(id, candidate);
            return candidate;
        }
    }

    @Override
    public void reset() {
        synchronized (monitor) {
            profiles.clear();
            subjects.clear();
            emails.clear();
            if (seedEnabled) {
                ProfileSeedData.profiles().forEach(this::insert);
            }
        }
    }

    private void insert(UserProfile profile) {
        if (profiles.putIfAbsent(profile.id(), profile) != null
                || subjects.putIfAbsent(profile.subject(), profile.id()) != null) {
            throw new IllegalStateException("Duplicate deterministic profile identity.");
        }
        String normalizedEmail = normalizeEmail(profile.email());
        if (emailUnique && emails.putIfAbsent(normalizedEmail, profile.id()) != null) {
            throw new IllegalStateException("Duplicate deterministic profile email.");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
