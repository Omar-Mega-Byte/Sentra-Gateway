package com.sentra.notification.notification;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Supported notification delivery channels exposed by the demonstration API.
 */
@Schema(description = "Notification channel.", allowableValues = {"EMAIL", "SMS", "PUSH", "WEBHOOK"})
public enum Channel {
    /** Email notification channel. */
    EMAIL,
    /** SMS notification channel. */
    SMS,
    /** Push notification channel. */
    PUSH,
    /** Webhook notification channel. */
    WEBHOOK
}
