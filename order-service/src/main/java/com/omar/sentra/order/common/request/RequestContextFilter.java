package com.omar.sentra.order.common.request;

import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.observability.OrderMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes response correlation and emits redacted normalized request logs.
 */
@Component("sentraOrderRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    private final int requestIdMaximum;
    private final Set<String> allowedRoutes;
    private final OrderMetrics metrics;

    public RequestContextFilter(OrderServiceProperties properties, OrderMetrics metrics) {
        requestIdMaximum = properties.gateway().requestIdMaxLength();
        allowedRoutes = Set.copyOf(properties.gateway().allowedRouteIds());
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = approvedOrGeneratedRequestId(request);
        request.setAttribute(RequestAttributes.REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        setCandidateRoute(request);
        String operation = operation(request);
        long started = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
            metrics.request(
                    operation,
                    response.getStatus() / 100 + "xx",
                    Duration.ofNanos(System.nanoTime() - started));
            log.info(
                    "request completed operation={} method={} statusClass={} durationMs={}",
                    operation,
                    request.getMethod(),
                    response.getStatus() / 100 + "xx",
                    elapsedMillis);
        }
    }

    private String approvedOrGeneratedRequestId(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(TrustedHeaders.REQUEST_ID));
        if (values.size() == 1) {
            String value = values.getFirst();
            if (!value.isEmpty()
                    && value.length() <= requestIdMaximum
                    && value.chars().allMatch(character -> character >= 0x21 && character <= 0x7E)) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    private void setCandidateRoute(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(TrustedHeaders.ROUTE_ID));
        if (values.size() == 1 && allowedRoutes.contains(values.getFirst())) {
            request.setAttribute(RequestAttributes.ROUTE_ID, values.getFirst());
        }
    }

    private static String operation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/internal/v1/orders".equals(path)) {
            return "POST".equals(request.getMethod()) ? "create" : "list";
        }
        if (path.matches("/internal/v1/orders/[^/]+")) {
            return "get";
        }
        if ("/internal/v1/admin/orders".equals(path)) {
            return "admin-list";
        }
        if (path.startsWith("/actuator/")) {
            return "management";
        }
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return "openapi";
        }
        return "unmapped";
    }
}
