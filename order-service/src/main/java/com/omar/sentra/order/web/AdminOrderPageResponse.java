package com.omar.sentra.order.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Paginated administrator order response.
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AdminOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages,
        @ArraySchema(schema = @Schema(implementation = AdminOrderResponse.class,
                requiredMode = Schema.RequiredMode.REQUIRED))
                List<AdminOrderResponse> items) {

    public AdminOrderPageResponse {
        items = List.copyOf(items);
    }
}
