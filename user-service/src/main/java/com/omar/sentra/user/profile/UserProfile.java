package com.omar.sentra.user.profile;

import java.time.Instant;
import java.util.UUID;

/**
 * Complete internal profile model. This type is never serialized directly by
 * an HTTP controller.
 *
 * @param id canonical profile identifier
 * @param subject stable identity-provider subject
 * @param displayName public display name
 * @param bio optional public biography
 * @param avatarUrl optional public HTTPS avatar URL
 * @param email private normalized email address
 * @param locale private BCP 47 locale
 * @param timezone private IANA time-zone ID
 * @param status internal lifecycle status
 * @param version optimistic concurrency version
 * @param createdAt creation time
 * @param updatedAt last mutation time
 */
public record UserProfile(
        UUID id,
        String subject,
        String displayName,
        String bio,
        String avatarUrl,
        String email,
        String locale,
        String timezone,
        ProfileStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
