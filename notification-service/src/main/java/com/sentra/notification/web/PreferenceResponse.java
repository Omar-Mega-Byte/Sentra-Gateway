package com.sentra.notification.web;

import com.sentra.notification.preference.NotificationPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Public preference DTO that excludes owner and gateway metadata.
 *
 * @param emailEnabled whether email notifications are enabled
 * @param smsEnabled whether SMS notifications are enabled
 * @param pushEnabled whether push notifications are enabled
 * @param webhookEnabled whether webhook notifications are enabled
 * @param version optimistic version after the update
 * @param updatedAt UTC update timestamp
 */
@Schema(description = "Notification preferences visible to the trusted user context.")
public record PreferenceResponse(
        @Schema(example = "true") boolean emailEnabled,
        @Schema(example = "false") boolean smsEnabled,
        @Schema(example = "true") boolean pushEnabled,
        @Schema(example = "false") boolean webhookEnabled,
        @Schema(example = "3", minimum = "0") int version,
        @Schema(type = "string", format = "date-time", description = "UTC preference update timestamp.") Instant updatedAt) {
    /** @param preferences internal preference state @return public response DTO */
    public static PreferenceResponse from(NotificationPreferences preferences) {
        return new PreferenceResponse(preferences.emailEnabled(), preferences.smsEnabled(), preferences.pushEnabled(), preferences.webhookEnabled(), preferences.version(), preferences.updatedAt());
    }
}
