package com.omar.sentra.gateway.security.ip;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * IPv4 and IPv6 exact/CIDR matcher.
 */
public final class CidrMatcher {
    private CidrMatcher() {
    }

    public static boolean matches(String network, String candidate) {
        try {
            String[] parts = network.split("/", 2);
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();
            byte[] candidateBytes = InetAddress.getByName(candidate).getAddress();
            if (networkBytes.length != candidateBytes.length) {
                return false;
            }
            int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : networkBytes.length * 8;
            if (prefix < 0 || prefix > networkBytes.length * 8) {
                return false;
            }
            int wholeBytes = prefix / 8;
            int remainingBits = prefix % 8;
            if (!Arrays.equals(
                    Arrays.copyOf(networkBytes, wholeBytes), Arrays.copyOf(candidateBytes, wholeBytes))) {
                return false;
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (networkBytes[wholeBytes] & mask) == (candidateBytes[wholeBytes] & mask);
        } catch (Exception exception) {
            return false;
        }
    }
}
