package com.omar.sentra.user.common.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkMatcherTest {

    @Test
    void matchesIpv4AndIpv6Cidrs() {
        NetworkMatcher matcher = new NetworkMatcher(List.of("10.20.0.0/16", "::1/128"));

        assertThat(matcher.matches("10.20.4.9")).isTrue();
        assertThat(matcher.matches("10.21.4.9")).isFalse();
        assertThat(matcher.matches("::1")).isTrue();
    }

    @Test
    void emptyMatcherIsNotConfigured() {
        assertThat(new NetworkMatcher(List.of()).configured()).isFalse();
    }
}
