package com.sentra.notification.common.request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable matcher for IP addresses, CIDR ranges, and startup-resolved service
 * names.
 */
public final class NetworkMatcher {
    private final List<Cidr> ranges;

    /**
     * Creates a matcher and resolves configured service names once at startup.
     *
     * @param entries IP literals, CIDRs, or service names
     */
    public NetworkMatcher(List<String> entries) {
        List<Cidr> parsed = new ArrayList<>();
        List<String> safeEntries = entries == null ? List.of() : entries;
        for (String rawEntry : safeEntries) {
            String entry = rawEntry == null ? "" : rawEntry.trim();
            if (entry.isBlank()) {
                continue;
            }
            if (entry.contains("/")) {
                parsed.add(Cidr.parse(entry));
            } else {
                try {
                    for (InetAddress address : InetAddress.getAllByName(entry)) {
                        parsed.add(Cidr.exact(address));
                    }
                } catch (UnknownHostException exception) {
                    throw new IllegalStateException("Unable to resolve configured network peer.", exception);
                }
            }
        }
        ranges = List.copyOf(parsed);
    }

    /** @return true when at least one peer rule is configured */
    public boolean configured() {
        return !ranges.isEmpty();
    }

    /**
     * Returns whether the remote address is allowed by any configured entry.
     *
     * @param remoteAddress servlet remote address
     * @return true when the remote address matches
     */
    public boolean matches(String remoteAddress) {
        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return ranges.stream().anyMatch(range -> range.matches(address));
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private record Cidr(byte[] network, int prefix) {
        static Cidr exact(InetAddress address) {
            byte[] bytes = address.getAddress();
            return new Cidr(bytes, bytes.length * 8);
        }

        static Cidr parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid configured CIDR.");
            }
            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int prefix = Integer.parseInt(parts[1]);
                int maximum = address.getAddress().length * 8;
                if (prefix < 0 || prefix > maximum) {
                    throw new IllegalStateException("Invalid configured CIDR prefix.");
                }
                return new Cidr(mask(address.getAddress(), prefix), prefix);
            } catch (UnknownHostException | NumberFormatException exception) {
                throw new IllegalStateException("Invalid configured CIDR.", exception);
            }
        }

        boolean matches(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            return java.util.Arrays.equals(network, mask(candidate, prefix));
        }

        private static byte[] mask(byte[] source, int prefix) {
            byte[] result = source.clone();
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            if (remainingBits > 0 && fullBytes < result.length) {
                int bitMask = 0xFF << (8 - remainingBits);
                result[fullBytes] = (byte) (result[fullBytes] & bitMask);
                fullBytes++;
            }
            for (int index = fullBytes; index < result.length; index++) {
                result[index] = 0;
            }
            return result;
        }
    }
}
