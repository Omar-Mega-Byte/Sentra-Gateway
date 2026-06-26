package com.sentra.notification.notification;

import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.config.NotificationServiceProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Handles validated owner-scoped notification reads.
 */
@Service
public class NotificationQueryService {
    private final NotificationRepository repository;
    private final NotificationServiceProperties properties;

    /** @param repository notification repository @param properties service configuration */
    public NotificationQueryService(NotificationRepository repository, NotificationServiceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Lists notifications for the trusted owner context. */
    public NotificationPage list(String tenantId, String subject, int page, int size, Optional<Channel> channel, Optional<NotificationStatus> status) {
        validatePage(page, size);
        return repository.findNotifications(tenantId, subject, channel, status, page, size);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || page > properties.limits().maxPageNumber()) {
            throw ServiceException.withDetails(ErrorCode.NTF_REQUEST_INVALID, List.of("page: must be between 0 and " + properties.limits().maxPageNumber()));
        }
        if (size < 1 || size > properties.limits().maxPageSize()) {
            throw ServiceException.withDetails(ErrorCode.NTF_REQUEST_INVALID, List.of("size: must be between 1 and " + properties.limits().maxPageSize()));
        }
    }
}
