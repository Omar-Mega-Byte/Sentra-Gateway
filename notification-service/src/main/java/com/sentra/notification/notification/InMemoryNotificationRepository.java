package com.sentra.notification.notification;

import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.config.NotificationServiceProperties;
import com.sentra.notification.observability.NotificationMetrics;
import com.sentra.notification.preference.NotificationPreferences;
import com.sentra.notification.preference.PreferenceUpdateCommand;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Synchronized deterministic memory repository used by the baseline service.
 */
@Repository
public class InMemoryNotificationRepository implements NotificationRepository {
    private static final Instant SEED_CREATED_ONE = Instant.parse("2026-06-01T10:00:00Z");
    private static final Instant SEED_CREATED_TWO = Instant.parse("2026-06-02T10:00:00Z");
    private static final Instant SEED_UPDATED = Instant.parse("2026-06-16T10:00:00Z");
    private final NotificationServiceProperties properties;
    private final NotificationMetrics metrics;
    private final Clock clock;
    private final List<Notification> notifications = new ArrayList<>();
    private final Map<OwnerKey, NotificationPreferences> preferences = new HashMap<>();
    private volatile boolean available = true;

    /** @param properties service configuration @param metrics bounded service metrics @param clock UTC clock */
    public InMemoryNotificationRepository(NotificationServiceProperties properties, NotificationMetrics metrics, Clock clock) {
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Rebuilds deterministic state at application startup. */
    @PostConstruct
    public synchronized void reset() {
        notifications.clear();
        preferences.clear();
        if (properties.repository().seedEnabled()) {
            seedNotifications();
            seedPreferences();
        }
        available = true;
    }

    @Override
    public synchronized NotificationPage findNotifications(String tenantId, String subject, Optional<Channel> channel, Optional<NotificationStatus> status, int page, int size) {
        if (!available) {
            metrics.recordRepositoryOperation("find-notifications", "unavailable");
            throw ServiceException.of(ErrorCode.NTF_DEPENDENCY_UNAVAILABLE);
        }
        List<Notification> matches = notifications.stream()
                .filter(notification -> sameOwner(notification.tenantId(), tenantId, notification.subject(), subject))
                .filter(notification -> channel.map(value -> value == notification.channel()).orElse(true))
                .filter(notification -> status.map(value -> value == notification.status()).orElse(true))
                .sorted(Comparator.comparing(Notification::createdAt).thenComparing(Notification::id).reversed())
                .toList();
        int fromIndex = Math.min(page * size, matches.size());
        int toIndex = Math.min(fromIndex + size, matches.size());
        int totalPages = matches.isEmpty() ? 0 : (int) Math.ceil((double) matches.size() / (double) size);
        metrics.recordRepositoryOperation("find-notifications", "success");
        return new NotificationPage(page, size, matches.size(), totalPages, matches.subList(fromIndex, toIndex));
    }

    @Override
    public synchronized NotificationPreferences updatePreferences(String tenantId, String subject, PreferenceUpdateCommand command) {
        if (!available) {
            metrics.recordRepositoryOperation("update-preferences", "unavailable");
            throw ServiceException.of(ErrorCode.NTF_DEPENDENCY_UNAVAILABLE);
        }
        OwnerKey key = new OwnerKey(normalizeTenant(tenantId), subject);
        NotificationPreferences current = preferences.get(key);
        int currentVersion = current == null ? 0 : current.version();
        if (command.expectedVersion() != currentVersion) {
            metrics.recordRepositoryOperation("update-preferences", "version-conflict");
            throw ServiceException.of(ErrorCode.NTF_VERSION_CONFLICT);
        }
        NotificationPreferences updated = new NotificationPreferences(key.tenantId(), subject, command.emailEnabled(), command.smsEnabled(), command.pushEnabled(), command.webhookEnabled(), currentVersion + 1, clock.instant());
        preferences.put(key, updated);
        metrics.recordRepositoryOperation("update-preferences", "success");
        return updated;
    }

    @Override
    public boolean available() {
        return available;
    }

    private boolean sameOwner(String storedTenant, String tenantId, String storedSubject, String subject) {
        return normalizeTenant(storedTenant).equals(normalizeTenant(tenantId)) && storedSubject.equals(subject);
    }

    private String normalizeTenant(String tenantId) {
        return tenantId == null ? "" : tenantId;
    }

    private void seedNotifications() {
        notifications.add(new Notification(UUID.fromString("70000000-0000-4000-8000-000000000001"), "sentra-user-omar", "tenant-demo", Channel.EMAIL, "Welcome to Sentra", "Your notification route is working.", NotificationStatus.SENT, SEED_CREATED_ONE));
        notifications.add(new Notification(UUID.fromString("70000000-0000-4000-8000-000000000002"), "sentra-user-omar", "tenant-demo", Channel.PUSH, "Resilience check queued", "The downstream notification read path has a queued fixture.", NotificationStatus.QUEUED, SEED_CREATED_TWO));
        notifications.add(new Notification(UUID.fromString("80000000-0000-4000-8000-000000000001"), "sentra-user-other", "tenant-demo", Channel.SMS, "Other subject fixture", "This record proves subject isolation.", NotificationStatus.SENT, SEED_CREATED_ONE));
        notifications.add(new Notification(UUID.fromString("90000000-0000-4000-8000-000000000001"), "sentra-user-omar", "tenant-other", Channel.EMAIL, "Other tenant fixture", "This record proves tenant isolation.", NotificationStatus.FAILED, SEED_CREATED_ONE));
    }

    private void seedPreferences() {
        preferences.put(new OwnerKey("tenant-demo", "sentra-user-omar"), new NotificationPreferences("tenant-demo", "sentra-user-omar", true, false, true, false, 2, SEED_UPDATED));
        preferences.put(new OwnerKey("tenant-demo", "sentra-user-other"), new NotificationPreferences("tenant-demo", "sentra-user-other", true, true, false, false, 1, SEED_UPDATED));
        preferences.put(new OwnerKey("tenant-other", "sentra-user-omar"), new NotificationPreferences("tenant-other", "sentra-user-omar", false, false, true, false, 1, SEED_UPDATED));
    }

    private record OwnerKey(String tenantId, String subject) {
    }
}
