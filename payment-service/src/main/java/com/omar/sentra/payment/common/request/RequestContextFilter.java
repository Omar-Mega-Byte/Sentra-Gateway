package com.omar.sentra.payment.common.request;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import com.omar.sentra.payment.observability.PaymentMetrics;
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
@Component("sentraPaymentRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    private final int requestIdMaximum;
    private final Set<String> allowedRoutes;
    private final PaymentMetrics metrics;

    public RequestContextFilter(PaymentServiceProperties properties, PaymentMetrics metrics) {
        requestIdMaximum = properties.gateway().requestIdMaxLength();
        allowedRoutes = Set.copyOf(properties.gateway().allowedRouteIds());
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
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
            Duration duration = Duration.ofNanos(System.nanoTime() - started);
            metrics.request(operation, response.getStatus() / 100 + "xx", duration);
            log.info(
                    "request completed operation={} method={} statusClass={} durationMs={}",
                    operation,
                    request.getMethod(),
                    response.getStatus() / 100 + "xx",
                    duration.toMillis());
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
        if (path.matches("/internal/v1/payments/[^/]+")) {
            return "payment-read";
        }
        if ("/internal/v1/payments".equals(path)) {
            return "payment-create";
        }
        if ("/internal/v1/refunds".equals(path)) {
            return "refund-create";
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
