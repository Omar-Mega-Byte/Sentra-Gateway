package com.omar.sentra.user.profile;

/**
 * Fully validated target values for a profile update.
 *
 * @param displayName target display name
 * @param bio target biography
 * @param avatarUrl target avatar URL
 * @param email target normalized email
 * @param locale target normalized locale
 * @param timezone target canonical time zone
 * @param expectedVersion optimistic version supplied by the caller
 */
public record ValidatedProfilePatch(
        String displayName,
        String bio,
        String avatarUrl,
        String email,
        String locale,
        String timezone,
        long expectedVersion) {}
