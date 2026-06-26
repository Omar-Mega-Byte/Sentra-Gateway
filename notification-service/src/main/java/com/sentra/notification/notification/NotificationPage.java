package com.sentra.notification.notification;

import java.util.List;

/**
 * Repository page result with deterministic total counters.
 *
 * @param page requested zero-based page number
 * @param size requested page size
 * @param totalElements total matching notifications
 * @param totalPages total pages for the requested size
 * @param items current page items
 */
public record NotificationPage(int page, int size, long totalElements, int totalPages, List<Notification> items) {
}
