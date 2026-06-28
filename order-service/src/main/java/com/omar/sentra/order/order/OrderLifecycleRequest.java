package com.omar.sentra.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Administrator lifecycle update for basic order, payment, and fulfillment state.
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record OrderLifecycleRequest(
        @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
                long version,
        OrderStatus status,
        PaymentStatus paymentStatus,
        FulfillmentStatus fulfillmentStatus) {}
