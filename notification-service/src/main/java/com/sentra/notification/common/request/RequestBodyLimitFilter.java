package com.sentra.notification.common.request;

import com.sentra.notification.common.error.ApiErrorSupport;
import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.config.NotificationServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the documented maximum request body size before domain work begins.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestBodyLimitFilter extends OncePerRequestFilter {
    private final NotificationServiceProperties properties;
    private final ApiErrorSupport errors;

    /** @param properties service configuration @param errors shared API error writer */
    public RequestBodyLimitFilter(NotificationServiceProperties properties, ApiErrorSupport errors) {
        this.properties = properties;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!hasBody(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        int limit = properties.limits().maxRequestBodyBytes();
        long contentLength = request.getContentLengthLong();
        if (contentLength > limit) {
            errors.write(request, response, ErrorCode.NTF_BODY_TOO_LARGE, ErrorCode.NTF_BODY_TOO_LARGE.status());
            return;
        }
        if (contentLength < 0) {
            byte[] body = readBoundedBody(request.getInputStream(), limit);
            if (body.length > limit) {
                errors.write(request, response, ErrorCode.NTF_BODY_TOO_LARGE, ErrorCode.NTF_BODY_TOO_LARGE.status());
                return;
            }
            filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private byte[] readBoundedBody(ServletInputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8192));
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            output.write(buffer, 0, read);
            if (total > limit) {
                break;
            }
        }
        return output.toByteArray();
    }
}
