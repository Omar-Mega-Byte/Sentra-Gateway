package com.sentra.notification.web;

import com.sentra.notification.notification.Channel;
import com.sentra.notification.notification.Notification;
import com.sentra.notification.notification.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Public notification DTO that excludes owner, provider, credential, retry, and
 * gateway metadata.
 *
 * @param id stable notification ID
 * @param channel notification channel
 * @param title notification title
 * @param message notification message
 * @param status notification status
 * @param createdAt UTC creation instant
 */
@Schema(description = "Notification visible to the trusted user context.")
public record NotificationResponse(
        @Schema(example = "70000000-0000-4000-8000-000000000001") UUID id,
        @Schema(example = "EMAIL") Channel channel,
        @Schema(example = "Welcome to Sentra", maxLength = 120) String title,
        @Schema(example = "Your notification route is working.", maxLength = 1000) String message,
        @Schema(example = "SENT") NotificationStatus status,
        @Schema(type = "string", format = "date-time", description = "UTC notification creation timestamp.") Instant createdAt) {
    /** @param notification internal notification @return public response DTO */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.id(), notification.channel(), notification.title(), notification.message(), notification.status(), notification.createdAt());
    }
}
