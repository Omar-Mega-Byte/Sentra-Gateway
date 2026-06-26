package com.sentra.notification.preference;

import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.notification.NotificationRepository;
import com.sentra.notification.observability.NotificationMetrics;
import org.springframework.stereotype.Service;

/**
 * Handles validated optimistic notification preference updates.
 */
@Service
public class PreferenceService {
    private final NotificationRepository repository;
    private final NotificationMetrics metrics;

    /** @param repository notification repository @param metrics bounded metrics recorder */
    public PreferenceService(NotificationRepository repository, NotificationMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    /** Updates preferences for the trusted owner. */
    public NotificationPreferences update(String tenantId, String subject, PreferenceUpdateCommand command) {
        try {
            NotificationPreferences preferences = repository.updatePreferences(tenantId, subject, command);
            metrics.recordPreferenceUpdate("success");
            return preferences;
        } catch (ServiceException exception) {
            metrics.recordPreferenceUpdate(exception.code().name().toLowerCase());
            throw exception;
        }
    }
}
