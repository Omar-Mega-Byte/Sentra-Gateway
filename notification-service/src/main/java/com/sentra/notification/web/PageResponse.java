package com.sentra.notification.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Stable page wrapper used by notification list responses.
 *
 * @param page zero-based page number
 * @param size page size
 * @param totalElements total matching elements
 * @param totalPages total pages
 * @param items page items
 * @param <T> item type
 */
@Schema(description = "Page wrapper for deterministic list responses.")
public record PageResponse<T>(
        @Schema(example = "0", minimum = "0") int page,
        @Schema(example = "20", minimum = "1", maximum = "100") int size,
        @Schema(example = "2", minimum = "0") long totalElements,
        @Schema(example = "1", minimum = "0") int totalPages,
        @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)) List<T> items) {
}
