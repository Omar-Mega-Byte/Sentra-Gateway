package com.omar.sentra.payment.common.request;

/**
 * Reserved trusted-context headers created by Sentra Gateway.
 */
public final class TrustedHeaders {
    public static final String REQUEST_ID = "X-Sentra-Request-Id";
    public static final String ACTOR_TYPE = "X-Sentra-Actor-Type";
    public static final String CLIENT_ID = "X-Sentra-Client-Id";
    public static final String KEY_ID = "X-Sentra-Key-Id";
    public static final String SCOPES = "X-Sentra-Scopes";
    public static final String ROUTE_ID = "X-Sentra-Route-Id";
    public static final String SIGNATURE_VERIFIED = "X-Sentra-Signature-Verified";
    public static final String SIGNATURE_KEY_ID = "X-Sentra-Signature-Key-Id";
    public static final String NONCE_STATUS = "X-Sentra-Nonce-Status";
    public static final String SOURCE_IP = "X-Sentra-Source-Ip";

    private TrustedHeaders() {}
}
