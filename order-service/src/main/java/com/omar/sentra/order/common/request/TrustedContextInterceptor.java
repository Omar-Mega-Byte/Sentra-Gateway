package com.omar.sentra.order.common.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authorizes documented order routes before controller argument parsing.
 */
@Component
public class TrustedContextInterceptor implements HandlerInterceptor {
    private final TrustedContextResolver resolver;

    public TrustedContextInterceptor(TrustedContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        TrustedRequestContext context = resolve(request);
        if (context != null) {
            request.setAttribute(RequestAttributes.TRUSTED_CONTEXT, context);
        }
        return true;
    }

    private TrustedRequestContext resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("/internal/v1/orders".equals(path) && "GET".equals(method)) {
            return resolver.requireUser(request, TrustedContextResolver.LIST_ROUTE, "orders:read");
        }
        if (path.matches("/internal/v1/orders/[^/]+") && "GET".equals(method)) {
            return resolver.requireUser(request, TrustedContextResolver.GET_ROUTE, "orders:read");
        }
        if (path.matches("/internal/v1/orders/[^/]+/cancel") && "POST".equals(method)) {
            return resolver.requireUser(request, TrustedContextResolver.CANCEL_ROUTE, "orders:write");
        }
        if ("/internal/v1/orders".equals(path) && "POST".equals(method)) {
            return resolver.requireUser(request, TrustedContextResolver.CREATE_ROUTE, "orders:write");
        }
        if ("/internal/v1/admin/orders".equals(path) && "GET".equals(method)) {
            return resolver.requireAdmin(request);
        }
        if (path.matches("/internal/v1/admin/orders/[^/]+") && "PATCH".equals(method)) {
            return resolver.requireAdmin(request, TrustedContextResolver.ADMIN_UPDATE_ROUTE);
        }
        return null;
    }
}
