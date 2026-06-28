package com.omar.sentra.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optimistic version body for a user order transition.
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record OrderVersionRequest(
        @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
                long version) {}
