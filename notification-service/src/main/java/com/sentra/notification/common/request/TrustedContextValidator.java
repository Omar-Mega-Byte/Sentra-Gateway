package com.sentra.notification.common.request;

import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.config.NotificationServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validates the sanitized trusted context forwarded by the gateway.
 */
@Component
public class TrustedContextValidator {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:\\-]{1,128}");
    private static final Pattern TRUSTED_VALUE_PATTERN = Pattern.compile("[\\p{Graph} ]+");
    private final NotificationServiceProperties properties;
    private final Environment environment;
    private final NetworkMatcher peerMatcher;

    /** @param properties service configuration @param environment active environment */
    public TrustedContextValidator(NotificationServiceProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
        peerMatcher = new NetworkMatcher(properties.gateway().allowedPeers());
    }

    /** Validates a user route that requires a specific scope. */
    public TrustedContext requireUser(HttpServletRequest request, String expectedRouteId, String requiredScope) {
        TrustedContext context = baseContext(request, expectedRouteId);
        if (!"USER".equals(context.actorType())) {
            throw ServiceException.of(ErrorCode.NTF_ACTOR_NOT_ALLOWED);
        }
        if (!context.scopes().contains(requiredScope)) {
            throw ServiceException.of(ErrorCode.NTF_SCOPE_REQUIRED);
        }
        return context;
    }

    /** Validates the admin test route that requires the notification admin role. */
    public TrustedContext requireAdmin(HttpServletRequest request, String expectedRouteId, String requiredRole) {
        TrustedContext context = baseContext(request, expectedRouteId);
        if (!"USER".equals(context.actorType())) {
            throw ServiceException.of(ErrorCode.NTF_ACTOR_NOT_ALLOWED);
        }
        if (!context.roles().contains(requiredRole)) {
            throw ServiceException.of(ErrorCode.NTF_ROLE_REQUIRED);
        }
        return context;
    }

    private TrustedContext baseContext(HttpServletRequest request, String expectedRouteId) {
        if (!properties.gateway().contextRequired()) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        rejectDuplicateTrustedHeaders(request);
        enforceConcretePeerRulesWhenConfigured(request);

        String requestId = requiredHeader(request, SentraHeaders.REQUEST_ID, properties.gateway().requestIdMaxLength());
        if (!REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        String subject = requiredHeader(request, SentraHeaders.SUBJECT, properties.gateway().subjectMaxLength());
        String actorType = requiredHeader(request, SentraHeaders.ACTOR_TYPE, properties.gateway().trustedHeaderMaxLength())
                .toUpperCase(Locale.ROOT);
        String routeId = requiredHeader(request, SentraHeaders.ROUTE_ID, properties.gateway().trustedHeaderMaxLength());
        String tenantId = optionalHeader(request, SentraHeaders.TENANT_ID, properties.gateway().tenantIdMaxLength());
        Set<String> scopes = splitHeader(optionalHeader(request, SentraHeaders.SCOPES, properties.gateway().trustedHeaderMaxLength()));
        Set<String> roles = splitHeader(optionalHeader(request, SentraHeaders.ROLES, properties.gateway().trustedHeaderMaxLength()));

        if (!TRUSTED_VALUE_PATTERN.matcher(subject).matches()
                || (tenantId != null && !TRUSTED_VALUE_PATTERN.matcher(tenantId).matches())) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        if (!expectedRouteId.equals(routeId) || !properties.gateway().allowedRouteIds().contains(routeId)) {
            throw ServiceException.of(ErrorCode.NTF_ROUTE_NOT_ALLOWED);
        }
        request.setAttribute(SentraHeaders.ATTR_REQUEST_ID, requestId);
        return new TrustedContext(requestId, subject, actorType, tenantId, scopes, roles, routeId);
    }

    private void rejectDuplicateTrustedHeaders(HttpServletRequest request) {
        for (String header : SentraHeaders.SECURITY_CRITICAL) {
            if (Collections.list(request.getHeaders(header)).size() > 1) {
                throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
            }
        }
    }

    private String requiredHeader(HttpServletRequest request, String header, int maxLength) {
        String value = optionalHeader(request, header, maxLength);
        if (value == null) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        return value;
    }

    private String optionalHeader(HttpServletRequest request, String header, int maxLength) {
        String value = request.getHeader(header);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.length() > Math.min(maxLength, properties.gateway().trustedHeaderMaxLength())) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        return trimmed;
    }

    private Set<String> splitHeader(String value) {
        if (value == null) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        for (String part : value.split("[,\\s]+")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        return Set.copyOf(values);
    }

    private void enforceConcretePeerRulesWhenConfigured(HttpServletRequest request) {
        if (properties.localOrTest(List.of(environment.getActiveProfiles()))) {
            return;
        }
        if (!peerMatcher.configured()) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
        if (!peerMatcher.matches(request.getRemoteAddr())) {
            throw ServiceException.of(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED);
        }
    }
}
