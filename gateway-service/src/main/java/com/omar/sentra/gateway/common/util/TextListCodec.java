package com.omar.sentra.gateway.common.util;

import java.util.Arrays;
import java.util.List;

/**
 * Encodes short controlled string lists for portable SQL text columns.
 */
public final class TextListCodec {
    private TextListCodec() {
    }

    public static String encode(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    public static List<String> decode(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\n")).filter(item -> !item.isBlank()).toList();
    }
}
