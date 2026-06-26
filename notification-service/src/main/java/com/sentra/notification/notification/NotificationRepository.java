package com.sentra.notification.notification;

import com.sentra.notification.preference.NotificationPreferences;
import com.sentra.notification.preference.PreferenceUpdateCommand;
import java.util.Optional;

/**
 * Repository abstraction for owner-scoped deterministic notification and
 * preference data.
 */
public interface NotificationRepository {
    /** Finds notifications owned by the trusted tenant/subject context. */
    NotificationPage findNotifications(String tenantId, String subject, Optional<Channel> channel, Optional<NotificationStatus> status, int page, int size);

    /** Updates preferences using optimistic versioning. */
    NotificationPreferences updatePreferences(String tenantId, String subject, PreferenceUpdateCommand command);

    /** @return true when available */
    boolean available();
}
