package com.omar.sentra.order.common.request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable matcher for IP addresses, CIDR ranges, and startup-resolved service
 * names.
 */
public final class NetworkMatcher {
    private final List<Cidr> ranges;

    /**
     * Creates a matcher and resolves service names once at startup.
     *
     * @param entries IP literals, CIDRs, or service names
     */
    public NetworkMatcher(List<String> entries) {
        List<Cidr> parsed = new ArrayList<>();
        for (String entry : entries) {
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

    public boolean configured() {
        return !ranges.isEmpty();
    }

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
            return candidate.length == network.length
                    && Arrays.equals(network, mask(candidate, prefix));
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
