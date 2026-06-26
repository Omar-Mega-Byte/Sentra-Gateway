package com.sentra.notification.preference;

import java.time.Instant;

/**
 * Immutable notification preference state for a single trusted tenant/subject
 * owner.
 *
 * @param tenantId trusted tenant identifier, when supplied
 * @param subject trusted owner subject
 * @param emailEnabled whether email notifications are enabled
 * @param smsEnabled whether SMS notifications are enabled
 * @param pushEnabled whether push notifications are enabled
 * @param webhookEnabled whether webhook notifications are enabled
 * @param version optimistic concurrency version
 * @param updatedAt UTC update instant
 */
public record NotificationPreferences(String tenantId, String subject, boolean emailEnabled, boolean smsEnabled, boolean pushEnabled, boolean webhookEnabled, int version, Instant updatedAt) {
}
