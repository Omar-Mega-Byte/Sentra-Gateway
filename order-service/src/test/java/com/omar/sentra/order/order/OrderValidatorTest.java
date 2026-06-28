package com.omar.sentra.order.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.order.TestProperties;
import com.omar.sentra.order.common.error.OrderServiceException;
import com.omar.sentra.order.web.CreateOrderItemRequest;
import com.omar.sentra.order.web.CreateOrderRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderValidatorTest {
    private final OrderValidator validator = new OrderValidator(TestProperties.defaults());

    @Test
    void trimsAndFingerprintsValidatedItemsDeterministically() {
        ValidatedCreateOrder first = validator.validate(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("  BOOK-JAVA-25  ", 1))));
        ValidatedCreateOrder second = validator.validate(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("BOOK-JAVA-25", 1))));

        assertThat(first.items()).containsExactly(new OrderItem("BOOK-JAVA-25", 1));
        assertThat(first.fingerprint()).isEqualTo(second.fingerprint()).hasSize(64);
    }

    @Test
    void rejectsDuplicateSkuAndInvalidQuantity() {
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest(List.of(
                        new CreateOrderItemRequest("SKU-ONE", 1),
                        new CreateOrderItemRequest("SKU-ONE", 0)))))
                .isInstanceOfSatisfying(OrderServiceException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("ORD_REQUEST_INVALID");
                    assertThat(exception.details()).extracting(detail -> detail.code())
                            .contains("duplicate", "range");
                });
    }
}
