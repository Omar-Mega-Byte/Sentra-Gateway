package com.omar.sentra.order.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Safe order item response.
 *
 * @param sku opaque SKU
 * @param quantity quantity
 */
@Schema(
        description = "Order item",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record OrderItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK-JAVA-25") String sku,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "1") int quantity) {}
