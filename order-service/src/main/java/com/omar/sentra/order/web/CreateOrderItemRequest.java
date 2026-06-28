package com.omar.sentra.order.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One client-supplied order item.
 *
 * @param sku trimmed visible ASCII SKU
 * @param quantity requested quantity
 */
@Schema(
        description = "One requested order item",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateOrderItemRequest(
        @Schema(minLength = 1, maxLength = 64, pattern = "^[!-~]+$",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK-JAVA-25")
                String sku,
        @Schema(minimum = "1", maximum = "100",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
                Integer quantity) {}
