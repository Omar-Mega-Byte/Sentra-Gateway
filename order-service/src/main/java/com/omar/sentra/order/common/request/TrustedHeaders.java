package com.omar.sentra.order.common.request;

/**
 * Reserved trusted-context headers created by Sentra Gateway.
 */
public final class TrustedHeaders {
    public static final String REQUEST_ID = "X-Sentra-Request-Id";
    public static final String SUBJECT = "X-Sentra-Subject";
    public static final String ACTOR_TYPE = "X-Sentra-Actor-Type";
    public static final String TENANT_ID = "X-Sentra-Tenant-Id";
    public static final String ROLES = "X-Sentra-Roles";
    public static final String SCOPES = "X-Sentra-Scopes";
    public static final String ROUTE_ID = "X-Sentra-Route-Id";
    public static final String SOURCE_IP = "X-Sentra-Source-Ip";
    public static final String AUTH_TIME = "X-Sentra-Auth-Time";
    public static final String CLIENT_ID = "X-Sentra-Client-Id";

    private TrustedHeaders() {}
}
