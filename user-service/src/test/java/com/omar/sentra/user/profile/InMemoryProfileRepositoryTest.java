package com.omar.sentra.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.user.TestProperties;
import com.omar.sentra.user.common.error.UserServiceException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryProfileRepositoryTest {
    private InMemoryProfileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProfileRepository(TestProperties.defaults());
    }

    @Test
    void resolvesSeedByIdAndSubject() {
        assertThat(repository.findById(ProfileSeedData.ACTIVE_ID)).isPresent();
        assertThat(repository.findBySubject(ProfileSeedData.ACTIVE_SUBJECT))
                .get()
                .extracting(UserProfile::id)
                .isEqualTo(ProfileSeedData.ACTIVE_ID);
    }

    @Test
    void atomicallyIncrementsAndRejectsStaleVersion() {
        UserProfile updated = repository.update(
                ProfileSeedData.ACTIVE_ID,
                3,
                current -> copy(current, "Changed", current.email(), 4));

        assertThat(updated.version()).isEqualTo(4);
        assertThatThrownBy(() -> repository.update(
                        ProfileSeedData.ACTIVE_ID,
                        3,
                        current -> current))
                .isInstanceOf(UserServiceException.class)
                .extracting(exception -> ((UserServiceException) exception).code())
                .isEqualTo("USR_VERSION_CONFLICT");
    }

    @Test
    void rejectsNormalizedEmailConflict() {
        assertThatThrownBy(() -> repository.update(
                        ProfileSeedData.ACTIVE_ID,
                        3,
                        current -> copy(current, current.displayName(), "DISABLED@example.test", 4)))
                .isInstanceOf(UserServiceException.class)
                .extracting(exception -> ((UserServiceException) exception).code())
                .isEqualTo("USR_EMAIL_CONFLICT");
    }

    @Test
    void resetRestoresDeterministicState() {
        repository.update(
                ProfileSeedData.ACTIVE_ID,
                3,
                current -> copy(current, "Changed", current.email(), 4));
        repository.reset();

        assertThat(repository.findById(ProfileSeedData.ACTIVE_ID))
                .get()
                .extracting(UserProfile::displayName, UserProfile::version)
                .containsExactly("Omar Hassan", 3L);
    }

    private static UserProfile copy(
            UserProfile current,
            String displayName,
            String email,
            long version) {
        return new UserProfile(
                current.id(),
                current.subject(),
                displayName,
                current.bio(),
                current.avatarUrl(),
                email,
                current.locale(),
                current.timezone(),
                current.status(),
                version,
                current.createdAt(),
                Instant.parse("2026-06-15T01:00:00Z"));
    }
}
