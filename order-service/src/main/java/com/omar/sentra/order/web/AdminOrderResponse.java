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
 * Administrator order representation with bounded owner references.
 *
 * @param id order ID
 * @param ownerSubject trusted owner subject
 * @param tenantId trusted nullable tenant
 * @param items ordered items
 * @param status order status
 * @param paymentStatus basic payment status
 * @param fulfillmentStatus basic fulfillment status
 * @param version optimistic version
 * @param createdAt creation time
 * @param updatedAt last update time
 */
@Schema(
        description = "Administrator order representation",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AdminOrderResponse(
        @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) UUID id,
        @Schema(minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "sentra-user-omar")
                String ownerSubject,
        @Schema(nullable = true, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "tenant-demo")
                String tenantId,
        @ArraySchema(minItems = 1, maxItems = 50,
                schema = @Schema(implementation = OrderItemResponse.class,
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY))
        List<OrderItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) PaymentStatus paymentStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) FulfillmentStatus fulfillmentStatus,
        @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) long version,
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) Instant createdAt,
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) Instant updatedAt) {

    public AdminOrderResponse {
        items = List.copyOf(items);
    }
}
