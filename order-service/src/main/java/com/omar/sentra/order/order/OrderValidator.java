package com.omar.sentra.order.order;

import static com.omar.sentra.order.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.order.common.error.ErrorDetail;
import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.web.CreateOrderItemRequest;
import com.omar.sentra.order.web.CreateOrderRequest;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates, normalizes, and fingerprints create-order input.
 */
@Component
public class OrderValidator {
    private final OrderServiceProperties.Limits limits;

    public OrderValidator(OrderServiceProperties properties) {
        limits = properties.limits();
    }

    /**
     * Produces canonical domain fields or throws a bounded safe validation error.
     *
     * @param request deserialized request
     * @return validated request
     */
    public ValidatedCreateOrder validate(CreateOrderRequest request) {
        List<ErrorDetail> details = new ArrayList<>();
        if (request == null || request.items() == null) {
            details.add(new ErrorDetail("items", "required", "At least one order item is required."));
            throw requestInvalid(details);
        }
        if (request.items().isEmpty() || request.items().size() > limits.maxItemsPerOrder()) {
            details.add(new ErrorDetail(
                    "items",
                    "size",
                    "Items must contain between 1 and " + limits.maxItemsPerOrder() + " entries."));
            throw requestInvalid(details);
        }

        List<OrderItem> items = new ArrayList<>(request.items().size());
        Set<String> skus = new HashSet<>();
        for (int index = 0; index < request.items().size(); index++) {
            CreateOrderItemRequest item = request.items().get(index);
            String field = "items[" + index + "]";
            if (item == null) {
                details.add(new ErrorDetail(field, "required", "Order items cannot be null."));
                continue;
            }
            String sku = item.sku() == null ? null : item.sku().trim();
            if (sku == null
                    || sku.isEmpty()
                    || sku.length() > limits.maxSkuLength()
                    || !visibleAscii(sku)) {
                details.add(new ErrorDetail(
                        field + ".sku",
                        "format",
                        "SKU must be 1-" + limits.maxSkuLength() + " visible ASCII characters."));
            } else if (!skus.add(sku)) {
                details.add(new ErrorDetail(
                        field + ".sku",
                        "duplicate",
                        "Duplicate SKUs are not allowed in one order."));
            }
            if (item.quantity() == null
                    || item.quantity() < 1
                    || item.quantity() > limits.maxItemQuantity()) {
                details.add(new ErrorDetail(
                        field + ".quantity",
                        "range",
                        "Quantity must be between 1 and " + limits.maxItemQuantity() + "."));
            }
            if (sku != null
                    && !sku.isEmpty()
                    && sku.length() <= limits.maxSkuLength()
                    && visibleAscii(sku)
                    && skus.contains(sku)
                    && item.quantity() != null
                    && item.quantity() >= 1
                    && item.quantity() <= limits.maxItemQuantity()) {
                items.add(new OrderItem(sku, item.quantity()));
            }
        }
        if (!details.isEmpty()) {
            throw requestInvalid(details);
        }
        return new ValidatedCreateOrder(items, fingerprint(items));
    }

    private static boolean visibleAscii(String value) {
        return value.chars().allMatch(character -> character >= 0x21 && character <= 0x7E);
    }

    private static String fingerprint(List<OrderItem> items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(items.size());
                for (OrderItem item : items) {
                    byte[] sku = item.sku().getBytes(StandardCharsets.UTF_8);
                    output.writeInt(sku.length);
                    output.write(sku);
                    output.writeInt(item.quantity());
                }
            }
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to fingerprint a validated order.", exception);
        }
    }
}
