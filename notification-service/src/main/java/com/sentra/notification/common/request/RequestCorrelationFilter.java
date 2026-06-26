package com.sentra.notification.common.request;

import com.sentra.notification.common.error.ApiErrorSupport;
import com.sentra.notification.config.NotificationServiceProperties;
import com.sentra.notification.observability.NotificationMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes request correlation, response cache policy, bounded metrics, and
 * redacted completion logging for internal API calls.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private final ApiErrorSupport errors;
    private final NotificationMetrics metrics;

    /** @param errors shared request ID resolver @param metrics bounded metrics recorder */
    public RequestCorrelationFilter(ApiErrorSupport errors, NotificationMetrics metrics) {
        this.errors = errors;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String requestId = errors.resolveOrCreateRequestId(request);
        String operation = operationName(request.getRequestURI());
        request.setAttribute(SentraHeaders.ATTR_OPERATION, operation);
        response.setHeader(SentraHeaders.RESPONSE_REQUEST_ID, requestId);
        if (request.getRequestURI().startsWith("/internal/")) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            String statusClass = (status / 100) + "xx";
            metrics.recordRequest(operation, statusClass, Duration.ofNanos(System.nanoTime() - start));
            log.info("notification-service completed requestId={} routeId={} operation={} statusClass={} result={}",
                    requestId,
                    safeRouteId(request),
                    operation,
                    statusClass,
                    status < 500 ? "completed" : "failed");
        }
    }

    private String operationName(String uri) {
        return switch (uri) {
            case "/internal/v1/notifications" -> "notifications-list";
            case "/internal/v1/preferences" -> "notification-preferences-update";
            case "/internal/v1/test" -> "admin-test-notification";
            default -> uri.startsWith("/actuator/") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")
                    ? "management"
                    : "unknown";
        };
    }

    private String safeRouteId(HttpServletRequest request) {
        String routeId = request.getHeader(SentraHeaders.ROUTE_ID);
        if (routeId == null) {
            return "absent";
        }
        return NotificationServiceProperties.DOCUMENTED_ROUTE_IDS.contains(routeId) ? routeId : "absent";
    }
}
