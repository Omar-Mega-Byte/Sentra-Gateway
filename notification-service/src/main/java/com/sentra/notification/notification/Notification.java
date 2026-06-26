package com.sentra.notification.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal immutable notification entity.
 *
 * @param id stable deterministic identifier
 * @param subject trusted owner subject
 * @param tenantId trusted tenant identifier, when supplied
 * @param channel delivery channel
 * @param title public notification title
 * @param message public notification body
 * @param status deterministic processing status
 * @param createdAt UTC creation instant
 */
public record Notification(UUID id, String subject, String tenantId, Channel channel, String title, String message, NotificationStatus status, Instant createdAt) {
}
