package com.omar.sentra.gateway.security.ip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CidrMatcherTest {
    @Test
    void matchesIpv4Networks() {
        assertThat(CidrMatcher.matches("192.168.10.0/24", "192.168.10.42")).isTrue();
        assertThat(CidrMatcher.matches("192.168.10.0/24", "192.168.11.42")).isFalse();
    }

    @Test
    void matchesIpv6Networks() {
        assertThat(CidrMatcher.matches("2001:db8::/32", "2001:db8::1")).isTrue();
        assertThat(CidrMatcher.matches("2001:db8::/32", "2001:db9::1")).isFalse();
    }

    @Test
    void rejectsInvalidNetworks() {
        assertThat(CidrMatcher.matches("not-a-network", "127.0.0.1")).isFalse();
        assertThat(CidrMatcher.matches("10.0.0.0/99", "10.0.0.1")).isFalse();
    }
}
