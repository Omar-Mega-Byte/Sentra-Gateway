package com.omar.sentra.payment.common.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authorizes documented payment routes before controller argument parsing.
 */
@Component
public class TrustedContextInterceptor implements HandlerInterceptor {
    private final TrustedContextResolver resolver;

    public TrustedContextInterceptor(TrustedContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TrustedRequestContext context = resolve(request);
        if (context != null) {
            request.setAttribute(RequestAttributes.TRUSTED_CONTEXT, context);
        }
        return true;
    }

    private TrustedRequestContext resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.matches("/internal/v1/payments/[^/]+") && "GET".equals(method)) {
            return resolver.requirePartner(
                    request,
                    TrustedContextResolver.PAYMENT_READ_ROUTE,
                    "payments:read",
                    false);
        }
        if ("/internal/v1/payments".equals(path) && "POST".equals(method)) {
            return resolver.requirePartner(
                    request,
                    TrustedContextResolver.PAYMENT_CREATE_ROUTE,
                    "payments:write",
                    true);
        }
        if ("/internal/v1/refunds".equals(path) && "POST".equals(method)) {
            return resolver.requirePartner(
                    request,
                    TrustedContextResolver.REFUND_CREATE_ROUTE,
                    "refunds:write",
                    true);
        }
        return null;
    }
}
