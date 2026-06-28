package com.omar.sentra.order.common.request;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Decodes the gateway comma-separated list format with backslash escaping.
 *
 * <p>Commas and backslashes inside values are escaped with a backslash. Empty
 * values, dangling escapes, and duplicate values are rejected.</p>
 */
public final class EscapedListCodec {
    private EscapedListCodec() {}

    /**
     * Decodes one trusted list header.
     *
     * @param encoded encoded header value
     * @return immutable decoded values
     * @throws IllegalArgumentException when the encoding is ambiguous
     */
    public static List<String> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < encoded.length(); index++) {
            char character = encoded.charAt(index);
            if (escaped) {
                if (character != ',' && character != '\\') {
                    throw new IllegalArgumentException("Unsupported list escape.");
                }
                current.append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == ',') {
                add(values, current);
            } else {
                current.append(character);
            }
        }
        if (escaped) {
            throw new IllegalArgumentException("Dangling list escape.");
        }
        add(values, current);
        if (new LinkedHashSet<>(values).size() != values.size()) {
            throw new IllegalArgumentException("Duplicate list values are not allowed.");
        }
        return List.copyOf(values);
    }

    private static void add(List<String> values, StringBuilder current) {
        String value = current.toString().trim();
        current.setLength(0);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty list values are not allowed.");
        }
        values.add(value);
    }
}
