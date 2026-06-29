package com.omar.sentra.payment.common.request;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Shared servlet request attributes for correlation and trusted routing.
 */
public final class RequestAttributes {
    public static final String REQUEST_ID = RequestAttributes.class.getName() + ".requestId";
    public static final String ROUTE_ID = RequestAttributes.class.getName() + ".routeId";
    public static final String TRUSTED_CONTEXT = RequestAttributes.class.getName() + ".trustedContext";

    private RequestAttributes() {}

    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ID);
        return value instanceof String text ? text : UUID.randomUUID().toString();
    }

    public static String routeId(HttpServletRequest request) {
        Object value = request.getAttribute(ROUTE_ID);
        return value instanceof String text ? text : null;
    }

    public static TrustedRequestContext trustedContext(HttpServletRequest request) {
        Object value = request.getAttribute(TRUSTED_CONTEXT);
        if (value instanceof TrustedRequestContext context) {
            return context;
        }
        throw new IllegalStateException("Trusted context was not established.");
    }
}
