package com.omar.sentra.user.profile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic, synthetic local and test profile fixtures.
 */
public final class ProfileSeedData {
    public static final UUID ACTIVE_ID = UUID.fromString("7aa99db8-a943-4b63-b4b7-79f769ef9f87");
    public static final String ACTIVE_SUBJECT = "sentra-user-omar";

    private ProfileSeedData() {}

    public static List<UserProfile> profiles() {
        Instant created = Instant.parse("2026-06-01T10:00:00Z");
        Instant updated = Instant.parse("2026-06-15T00:00:00Z");
        return List.of(
                new UserProfile(
                        ACTIVE_ID,
                        ACTIVE_SUBJECT,
                        "Omar Hassan",
                        "Backend engineer",
                        "https://cdn.example.test/avatars/7aa99db8.png",
                        "omar@example.test",
                        "en-EG",
                        "Africa/Cairo",
                        ProfileStatus.ACTIVE,
                        3,
                        created,
                        updated),
                new UserProfile(
                        UUID.fromString("11111111-1111-4111-8111-111111111111"),
                        "sentra-user-disabled",
                        "Disabled Profile",
                        null,
                        null,
                        "disabled@example.test",
                        "en-US",
                        "UTC",
                        ProfileStatus.DISABLED,
                        1,
                        created,
                        updated),
                new UserProfile(
                        UUID.fromString("22222222-2222-4222-8222-222222222222"),
                        "sentra-user-deleted",
                        "Deleted Profile",
                        null,
                        null,
                        "deleted@example.test",
                        "en-US",
                        "UTC",
                        ProfileStatus.DELETED,
                        1,
                        created,
                        updated));
    }
}
