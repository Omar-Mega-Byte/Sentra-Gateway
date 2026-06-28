package com.omar.sentra.order.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Strict create-order request. Ownership and all server-controlled fields are
 * intentionally absent.
 *
 * @param items ordered item list
 */
@Schema(
        description = "Strict create-order request",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateOrderRequest(
        @ArraySchema(minItems = 1, maxItems = 50,
                schema = @Schema(implementation = CreateOrderItemRequest.class,
                        requiredMode = Schema.RequiredMode.REQUIRED))
                List<CreateOrderItemRequest> items) {}
