package com.sentra.notification.common.error;

import com.sentra.notification.common.request.SentraHeaders;
import com.sentra.notification.config.NotificationServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds and writes redacted {@link ApiError} instances consistently across
 * controller advice and servlet filters.
 */
@Component
public class ApiErrorSupport {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:\\-]{1,128}");
    private final NotificationServiceProperties properties;
    private final Clock clock;
    private final JsonMapper jsonMapper;

    /**
     * Creates API error support.
     *
     * @param properties service configuration
     * @param clock UTC clock for error timestamps
     * @param jsonMapper JSON writer
     */
    public ApiErrorSupport(NotificationServiceProperties properties, Clock clock, JsonMapper jsonMapper) {
        this.properties = properties;
        this.clock = clock;
        this.jsonMapper = jsonMapper;
    }

    /** Creates a redacted API error for the current request. */
    public ApiError create(HttpServletRequest request, ErrorCode code, int status, List<String> details) {
        String requestId = resolveOrCreateRequestId(request);
        return new ApiError(
                clock.instant(),
                requestId,
                status,
                code.name(),
                code.message(),
                request.getRequestURI(),
                safeRouteId(request),
                details.stream().limit(properties.limits().maxErrorDetails()).toList());
    }

    /** Writes a redacted JSON error directly from a servlet filter. */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, int status) throws IOException {
        ApiError error = create(request, code, status, List.of());
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(SentraHeaders.RESPONSE_REQUEST_ID, error.requestId());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        jsonMapper.writeValue(response.getOutputStream(), error);
    }

    /** Resolves a bounded request ID or creates a non-sensitive fallback. */
    public String resolveOrCreateRequestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(SentraHeaders.ATTR_REQUEST_ID);
        if (attribute instanceof String requestId && validRequestId(requestId)) {
            return requestId;
        }
        String header = request.getHeader(SentraHeaders.REQUEST_ID);
        String requestId = validRequestId(header) ? header.trim() : UUID.randomUUID().toString();
        request.setAttribute(SentraHeaders.ATTR_REQUEST_ID, requestId);
        return requestId;
    }

    private boolean validRequestId(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= properties.gateway().requestIdMaxLength()
                && REQUEST_ID_PATTERN.matcher(value.trim()).matches();
    }

    private String safeRouteId(HttpServletRequest request) {
        String routeId = request.getHeader(SentraHeaders.ROUTE_ID);
        if (routeId == null) {
            return null;
        }
        String trimmed = routeId.trim();
        return NotificationServiceProperties.DOCUMENTED_ROUTE_IDS.contains(trimmed) ? trimmed : null;
    }
}
