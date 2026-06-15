package com.omar.sentra.user.common.request;

import com.omar.sentra.user.config.UserServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes response correlation and emits redacted, normalized request logs.
 */
@Component("sentraRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    private final UserServiceProperties properties;

    public RequestContextFilter(UserServiceProperties properties) {
        this.properties = properties;
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
        String operation = operation(request);
        long started = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
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
            boolean valid = !value.isEmpty()
                    && value.length() <= properties.gateway().requestIdMaxLength()
                    && value.chars().allMatch(character -> character >= 0x21 && character <= 0x7E);
            if (valid) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    private static String operation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.matches("/internal/v1/users/[^/]+/public")) {
            return "public-profile-read";
        }
        if ("/internal/v1/users/me".equals(path)) {
            return "PATCH".equals(request.getMethod()) ? "profile-update" : "profile-read";
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
