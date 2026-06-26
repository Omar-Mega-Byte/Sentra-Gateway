package com.omar.sentra.user.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Safe private profile returned only for the trusted current subject.
 *
 * @param id profile identifier
 * @param displayName display name
 * @param bio optional biography
 * @param avatarUrl optional avatar URL
 * @param email private email address
 * @param locale BCP 47 locale
 * @param timezone IANA time-zone ID
 * @param version optimistic concurrency version
 * @param createdAt creation time
 * @param updatedAt last update time
 */
@Schema(description = "Safe private profile for the trusted current subject")
public record CurrentProfileResponse(
        @Schema(format = "uuid", example = "7aa99db8-a943-4b63-b4b7-79f769ef9f87") UUID id,
        @Schema(minLength = 1, maxLength = 100, example = "Omar Hassan") String displayName,
        @Schema(nullable = true, maxLength = 500, example = "Backend engineer") String bio,
        @Schema(nullable = true, format = "uri", maxLength = 2048) String avatarUrl,
        @Schema(format = "email", maxLength = 254, example = "omar@example.test") String email,
        @Schema(maxLength = 35, example = "en-EG") String locale,
        @Schema(maxLength = 64, example = "Africa/Cairo") String timezone,
        @Schema(minimum = "1", example = "3") long version,
        @Schema(format = "date-time", example = "2026-06-01T10:00:00Z") Instant createdAt,
        @Schema(format = "date-time", example = "2026-06-15T00:00:00Z") Instant updatedAt) {}
