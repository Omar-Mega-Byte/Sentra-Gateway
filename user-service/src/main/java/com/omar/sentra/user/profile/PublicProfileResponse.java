package com.omar.sentra.user.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Explicit allowlist of fields safe for anonymous public profile responses.
 *
 * @param id profile identifier
 * @param displayName display name
 * @param bio optional biography
 * @param avatarUrl optional avatar URL
 */
@Schema(description = "Public profile containing only explicitly allowlisted fields")
public record PublicProfileResponse(
        @Schema(format = "uuid", example = "7aa99db8-a943-4b63-b4b7-79f769ef9f87") UUID id,
        @Schema(minLength = 1, maxLength = 100, example = "Omar Hassan") String displayName,
        @Schema(nullable = true, maxLength = 500, example = "Backend engineer") String bio,
        @Schema(
                        nullable = true,
                        format = "uri",
                        maxLength = 2048,
                        example = "https://cdn.example.test/avatars/7aa99db8.png")
                String avatarUrl) {}
