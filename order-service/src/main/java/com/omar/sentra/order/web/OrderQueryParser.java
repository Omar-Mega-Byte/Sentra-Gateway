package com.omar.sentra.order.web;

import static com.omar.sentra.order.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.order.common.error.ErrorDetail;
import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.order.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Strictly parses the bounded collection query contract.
 */
@Component
public class OrderQueryParser {
    private static final Set<String> USER_PARAMETERS = Set.of("page", "size", "status");
    private static final Set<String> ADMIN_PARAMETERS =
            Set.of("page", "size", "status", "tenantId", "subject");

    private final OrderServiceProperties properties;

    public OrderQueryParser(OrderServiceProperties properties) {
        this.properties = properties;
    }

    public Query parseUser(HttpServletRequest request) {
        rejectUnknown(request, USER_PARAMETERS);
        return new Query(
                integer(request, "page", 0, 0, properties.limits().maxPageNumber()),
                integer(request, "size", properties.limits().defaultPageSize(),
                        1, properties.limits().maxPageSize()),
                status(request),
                null,
                null);
    }

    public Query parseAdmin(HttpServletRequest request) {
        rejectUnknown(request, ADMIN_PARAMETERS);
        return new Query(
                integer(request, "page", 0, 0, properties.limits().maxPageNumber()),
                integer(request, "size", properties.limits().defaultPageSize(),
                        1, properties.limits().maxPageSize()),
                status(request),
                text(request, "tenantId", properties.gateway().tenantIdMaxLength()),
                text(request, "subject", properties.gateway().subjectMaxLength()));
    }

    private static void rejectUnknown(HttpServletRequest request, Set<String> allowed) {
        for (String parameter : request.getParameterMap().keySet()) {
            if (!allowed.contains(parameter)) {
                throw invalid(parameter, "unknown", "Unknown query parameters are not supported.");
            }
        }
    }

    private static int integer(
            HttpServletRequest request,
            String name,
            int defaultValue,
            int minimum,
            int maximum) {
        String value = single(request, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new NumberFormatException("out of range");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(name, "range", name + " is outside the supported range.");
        }
    }

    private static OrderStatus status(HttpServletRequest request) {
        String value = single(request, "status");
        if (value == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("status", "enum", "Status must be a documented uppercase order status.");
        }
    }

    private static String text(HttpServletRequest request, String name, int maximumLength) {
        String value = single(request, name);
        if (value == null) {
            return null;
        }
        if (value.isBlank()
                || value.length() > maximumLength
                || value.chars().anyMatch(character -> character < 0x20 || character > 0x7E)) {
            throw invalid(name, "format", name + " is not a valid exact filter.");
        }
        return value;
    }

    private static String single(HttpServletRequest request, String name) {
        Map<String, String[]> parameters = request.getParameterMap();
        String[] values = parameters.get(name);
        if (values == null) {
            return null;
        }
        if (values.length != 1 || values[0].isBlank()) {
            throw invalid(name, "single_value", name + " must have one nonblank value.");
        }
        return values[0];
    }

    private static RuntimeException invalid(String field, String code, String message) {
        return requestInvalid(List.of(new ErrorDetail(field, code, message)));
    }

    /**
     * Parsed bounded query.
     */
    public record Query(
            int page,
            int size,
            OrderStatus status,
            String tenantId,
            String subject) {}
}
