package com.omar.sentra.gateway.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextListCodecTest {
    @Test
    void roundTripsControlledValues() {
        List<String> values = List.of("orders:read", "orders:write");
        assertThat(TextListCodec.decode(TextListCodec.encode(values))).isEqualTo(values);
    }

    @Test
    void handlesEmptyValues() {
        assertThat(TextListCodec.decode(null)).isEmpty();
        assertThat(TextListCodec.decode("")).isEmpty();
        assertThat(TextListCodec.encode(null)).isEmpty();
    }
}
