package com.omar.sentra.gateway.common.request;

/**
 * Exchange attribute names shared by gateway filters.
 */
public final class RequestAttributes {
    public static final String REQUEST_ID = "sentra.requestId";
    public static final String START_NANOS = "sentra.startNanos";
    public static final String CLIENT_IP = "sentra.clientIp";
    public static final String API_KEY_PRINCIPAL = "sentra.apiKeyPrincipal";
    public static final String DECISION = "sentra.decision";
    public static final String REASON_CODE = "sentra.reasonCode";

    private RequestAttributes() {
    }
}
