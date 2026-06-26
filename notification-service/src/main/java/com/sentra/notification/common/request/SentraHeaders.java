package com.sentra.notification.common.request;

import java.util.List;

/**
 * Header names consumed from the gateway's trusted downstream context.
 */
public final class SentraHeaders {
    /** Gateway-approved request identifier header. */
    public static final String REQUEST_ID = "X-Sentra-Request-Id";
    /** Trusted JWT subject header. */
    public static final String SUBJECT = "X-Sentra-Subject";
    /** Trusted actor type header. */
    public static final String ACTOR_TYPE = "X-Sentra-Actor-Type";
    /** Trusted tenant identifier header. */
    public static final String TENANT_ID = "X-Sentra-Tenant-Id";
    /** Trusted scope set header. */
    public static final String SCOPES = "X-Sentra-Scopes";
    /** Trusted role set header. */
    public static final String ROLES = "X-Sentra-Roles";
    /** Trusted route identifier header. */
    public static final String ROUTE_ID = "X-Sentra-Route-Id";
    /** Local/test artificial delay header. */
    public static final String TEST_DELAY_MILLIS = "X-Sentra-Test-Delay-Millis";
    /** Local/test status override header. */
    public static final String TEST_STATUS = "X-Sentra-Test-Status";
    /** Local/test malformed response header. */
    public static final String TEST_MALFORMED = "X-Sentra-Test-Malformed";
    /** Local/test disconnect simulation header. */
    public static final String TEST_DISCONNECT = "X-Sentra-Test-Disconnect";
    /** Response correlation header. */
    public static final String RESPONSE_REQUEST_ID = "X-Request-Id";
    /** Servlet request attribute containing the response request ID. */
    public static final String ATTR_REQUEST_ID = "sentra.requestId";
    /** Servlet request attribute containing the finite operation name. */
    public static final String ATTR_OPERATION = "sentra.operation";
    /** Security-critical headers that must not be duplicated. */
    public static final List<String> SECURITY_CRITICAL = List.of(
            REQUEST_ID, SUBJECT, ACTOR_TYPE, TENANT_ID, SCOPES, ROLES, ROUTE_ID,
            TEST_DELAY_MILLIS, TEST_STATUS, TEST_MALFORMED, TEST_DISCONNECT);

    private SentraHeaders() {
    }
}
