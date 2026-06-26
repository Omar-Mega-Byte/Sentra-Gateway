package com.sentra.notification.notification;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Stable state values for deterministic notification records.
 */
@Schema(description = "Notification processing status.", allowableValues = {"QUEUED", "SENT", "FAILED", "SUPPRESSED"})
public enum NotificationStatus {
    /** The notification is queued by the demonstration repository. */
    QUEUED,
    /** The notification is marked as sent by deterministic data. */
    SENT,
    /** The notification is marked as failed by deterministic data. */
    FAILED,
    /** The notification was suppressed and not delivered. */
    SUPPRESSED
}
