package com.omar.sentra.order.web;

import com.omar.sentra.order.order.OrderStatus;
import com.omar.sentra.order.order.PaymentStatus;
import com.omar.sentra.order.order.FulfillmentStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User-facing order representation without owner or tenant identity.
 *
 * @param id order ID
 * @param items ordered items
 * @param status order status
 * @param paymentStatus basic payment status
 * @param fulfillmentStatus basic fulfillment status
 * @param version optimistic version
 * @param createdAt creation time
 * @param updatedAt last update time
 */
@Schema(
        description = "Owner-safe user order representation",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record UserOrderResponse(
        @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "10000000-0000-4000-8000-000000000001") UUID id,
        @ArraySchema(minItems = 1, maxItems = 50,
                schema = @Schema(implementation = OrderItemResponse.class,
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY))
        List<OrderItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "COMPLETED") OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "PAID") PaymentStatus paymentStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "DELIVERED") FulfillmentStatus fulfillmentStatus,
        @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "2") long version,
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "2026-06-01T10:00:00Z") Instant createdAt,
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "2026-06-02T09:30:00Z") Instant updatedAt) {

    public UserOrderResponse {
        items = List.copyOf(items);
    }
}
