package com.omar.sentra.order.common.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkMatcherTest {

    @Test
    void matchesExactAndCidrAddresses() {
        NetworkMatcher matcher = new NetworkMatcher(List.of("127.0.0.1", "10.20.0.0/16", "::1/128"));

        assertThat(matcher.matches("127.0.0.1")).isTrue();
        assertThat(matcher.matches("10.20.9.8")).isTrue();
        assertThat(matcher.matches("10.21.9.8")).isFalse();
        assertThat(matcher.matches("::1")).isTrue();
    }

    @Test
    void rejectsInvalidConfiguredCidr() {
        assertThatThrownBy(() -> new NetworkMatcher(List.of("10.0.0.0/99")))
                .isInstanceOf(IllegalStateException.class);
    }
}
