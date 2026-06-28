package com.omar.sentra.order.common.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EscapedListCodecTest {

    @Test
    void decodesEscapedValues() {
        assertThat(EscapedListCodec.decode("orders:read,role\\,special,path\\\\value"))
                .isEqualTo(List.of("orders:read", "role,special", "path\\value"));
    }

    @Test
    void rejectsAmbiguousValues() {
        for (String value : List.of("a,,b", "a\\", "a\\xb", "a,a")) {
            assertThatThrownBy(() -> EscapedListCodec.decode(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
