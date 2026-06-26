package com.omar.sentra.user.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.user.TestProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartupValidatorTest {

    @Test
    void acceptsLocalAndTestProfiles() {
        assertThatCode(() -> StartupValidator.validate(
                        new String[] {"local"},
                        TestProperties.defaults()))
                .doesNotThrowAnyException();
    }

    @Test
    void requiresExplicitProfile() {
        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[0],
                        TestProperties.defaults()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnsafeProductionSettings() {
        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[] {"prod"},
                        TestProperties.create(
                                false,
                                false,
                                List.of("user-profile-read"),
                                false,
                                false,
                                true,
                                List.of("10.0.0.0/8"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trusted context");
    }

    @Test
    void acceptsHardenedProductionSettings() {
        assertThatCode(() -> StartupValidator.validate(
                        new String[] {"prod"},
                        TestProperties.create(
                                false,
                                true,
                                List.of("user-public-profile", "user-profile-read", "user-profile-update"),
                                false,
                                false,
                                true,
                                List.of("10.0.0.0/8"))))
                .doesNotThrowAnyException();
    }

    @Test
    void mixedDevelopmentAndProductionProfilesRemainProductionLike() {
        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[] {"local", "prod"},
                        TestProperties.defaults()))
                .isInstanceOf(IllegalStateException.class);
    }
}
