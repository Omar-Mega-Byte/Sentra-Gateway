package com.omar.sentra.order.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Paginated user order response.
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record UserOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages,
        @ArraySchema(schema = @Schema(implementation = UserOrderResponse.class,
                requiredMode = Schema.RequiredMode.REQUIRED))
                List<UserOrderResponse> items) {

    public UserOrderPageResponse {
        items = List.copyOf(items);
    }
}
