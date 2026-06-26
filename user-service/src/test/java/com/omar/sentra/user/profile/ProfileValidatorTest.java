package com.omar.sentra.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.user.TestProperties;
import com.omar.sentra.user.common.error.UserServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileValidatorTest {
    private ProfileValidator validator;
    private UserProfile current;

    @BeforeEach
    void setUp() {
        validator = new ProfileValidator(TestProperties.defaults());
        current = ProfileSeedData.profiles().getFirst();
    }

    @Test
    void normalizesMutableValues() {
        ProfilePatchRequest request = new ProfilePatchRequest();
        request.setDisplayName("  Omar H. ");
        request.setEmail(" OMAR.H@EXAMPLE.TEST ");
        request.setLocale("en-eg");
        request.setTimezone("Africa/Cairo");
        request.setVersion(3L);

        ValidatedProfilePatch patch = validator.validate(request, current);

        assertThat(patch.displayName()).isEqualTo("Omar H.");
        assertThat(patch.email()).isEqualTo("omar.h@example.test");
        assertThat(patch.locale()).isEqualTo("en-EG");
    }

    @Test
    void allowsExplicitNullOnlyForNullableFields() {
        ProfilePatchRequest request = new ProfilePatchRequest();
        request.setBio(null);
        request.setAvatarUrl(null);
        request.setVersion(3L);

        assertThat(validator.validate(request, current))
                .extracting(ValidatedProfilePatch::bio, ValidatedProfilePatch::avatarUrl)
                .containsExactly(null, null);

        request = new ProfilePatchRequest();
        request.setEmail(null);
        request.setVersion(3L);
        ProfilePatchRequest invalid = request;
        assertThatThrownBy(() -> validator.validate(invalid, current))
                .isInstanceOf(UserServiceException.class);
    }

    @Test
    void rejectsUnsafeAvatarUrls() {
        for (String value : new String[] {
            "http://cdn.example.test/a.png",
            "https://localhost/a.png",
            "https://169.254.169.254/latest/meta-data",
            "https://user:secret@cdn.example.test/a.png",
            "https://cdn.example.test/a.png#fragment"
        }) {
            ProfilePatchRequest request = new ProfilePatchRequest();
            request.setAvatarUrl(value);
            request.setVersion(3L);
            assertThatThrownBy(() -> validator.validate(request, current))
                    .isInstanceOf(UserServiceException.class);
        }
    }

    @Test
    void rejectsEmptyPatchAndInvalidVersion() {
        ProfilePatchRequest request = new ProfilePatchRequest();
        request.setVersion(0L);

        assertThatThrownBy(() -> validator.validate(request, current))
                .isInstanceOf(UserServiceException.class)
                .extracting(exception -> ((UserServiceException) exception).code())
                .isEqualTo("USR_REQUEST_INVALID");
    }
}
