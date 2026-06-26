package com.omar.sentra.user.common.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EscapedListCodecTest {

    @Test
    void decodesNormalAndEscapedValues() {
        assertThat(EscapedListCodec.decode("profile:read,team\\,blue,path\\\\value"))
                .isEqualTo(List.of("profile:read", "team,blue", "path\\value"));
    }

    @Test
    void returnsEmptyListForBlankInput() {
        assertThat(EscapedListCodec.decode(" ")).isEmpty();
    }

    @Test
    void rejectsDuplicateAndEmptyValues() {
        assertThatThrownBy(() -> EscapedListCodec.decode("a,a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscapedListCodec.decode("a,,b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedOrDanglingEscapes() {
        assertThatThrownBy(() -> EscapedListCodec.decode("a\\q"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EscapedListCodec.decode("a\\"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
