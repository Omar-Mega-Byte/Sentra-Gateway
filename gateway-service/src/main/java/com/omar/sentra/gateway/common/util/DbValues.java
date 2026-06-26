package com.omar.sentra.gateway.common.util;

import io.r2dbc.spi.Readable;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Portable R2DBC row conversion helpers.
 */
public final class DbValues {
    private DbValues() {
    }

    public static Instant instant(Readable row, String column) {
        Object value = row.get(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value: " + value.getClass());
    }
}
