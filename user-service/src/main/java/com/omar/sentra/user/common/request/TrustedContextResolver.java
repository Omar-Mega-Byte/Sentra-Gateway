package com.omar.sentra.user.common.request;

import static com.omar.sentra.user.common.error.ServiceErrors.actorNotAllowed;
import static com.omar.sentra.user.common.error.ServiceErrors.scopeRequired;
import static com.omar.sentra.user.common.error.ServiceErrors.trustedContextRequired;

import com.omar.sentra.user.config.UserServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates trusted gateway headers and resolves the subject used by `/me`.
 */
@Component
public class TrustedContextResolver {
    public static final String PUBLIC_ROUTE = "user-public-profile";
    public static final String READ_ROUTE = "user-profile-read";
    public static final String UPDATE_ROUTE = "user-profile-update";

    private static final Pattern ACTOR_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");

    private final UserServiceProperties properties;
    private final Set<String> allowedRouteIds;
    private final NetworkMatcher peerMatcher;

    public TrustedContextResolver(UserServiceProperties properties) {
        this.properties = properties;
        this.allowedRouteIds = Set.copyOf(properties.gateway().allowedRouteIds());
        this.peerMatcher = new NetworkMatcher(properties.gateway().allowedPeers());
    }

    /**
     * Validates context for public profile lookup.
     *
     * @param request servlet request
     * @return validated public request context
     */
    public TrustedRequestContext requirePublic(HttpServletRequest request) {
        validatePeer(request);
        String requestId = trustedRequestId(request, properties.gateway().contextRequired());
        String routeId = routeId(request, PUBLIC_ROUTE, properties.gateway().contextRequired());
        validateOptionalHeaders(request);
        validateOptional(request, TrustedHeaders.SUBJECT, properties.gateway().trustedHeaderMaxLength(), false);
        String actor = optionalSingle(request, TrustedHeaders.ACTOR_TYPE);
        if (actor != null && !ACTOR_PATTERN.matcher(actor).matches()) {
            throw trustedContextRequired();
        }
        validateOptional(request, TrustedHeaders.SCOPES, properties.gateway().trustedHeaderMaxLength(), true);
        validateOptional(request, TrustedHeaders.CLIENT_ID, properties.gateway().trustedHeaderMaxLength(), false);
        return new TrustedRequestContext(requestId, null, null, List.of(), routeId);
    }

    /**
     * Validates context for an authenticated self-service operation.
     *
     * @param request servlet request
     * @param expectedRouteId required gateway route ID
     * @param expectedScope required user scope
     * @return validated user context
     */
    public TrustedRequestContext requireUser(
            HttpServletRequest request,
            String expectedRouteId,
            String expectedScope) {
        validatePeer(request);
        String requestId = trustedRequestId(request, properties.gateway().contextRequired());
        String subject = requiredSingle(request, TrustedHeaders.SUBJECT);
        String actorType = requiredSingle(request, TrustedHeaders.ACTOR_TYPE);
        if (!ACTOR_PATTERN.matcher(actorType).matches()) {
            throw trustedContextRequired();
        }
        if (!"USER".equals(actorType)) {
            throw actorNotAllowed();
        }
        if (headerValues(request, TrustedHeaders.CLIENT_ID).size() > 0) {
            throw actorNotAllowed();
        }
        List<String> scopes;
        try {
            scopes = EscapedListCodec.decode(requiredSingle(request, TrustedHeaders.SCOPES));
        } catch (IllegalArgumentException exception) {
            throw trustedContextRequired();
        }
        if (!scopes.contains(expectedScope)) {
            throw scopeRequired();
        }
        String routeId = routeId(request, expectedRouteId, properties.gateway().contextRequired());
        validateOptionalHeaders(request);
        return new TrustedRequestContext(requestId, subject, actorType, scopes, routeId);
    }

    private void validatePeer(HttpServletRequest request) {
        if (peerMatcher.configured() && !peerMatcher.matches(request.getRemoteAddr())) {
            throw trustedContextRequired();
        }
        if (!properties.gateway().contextRequired()
                && !peerMatcher.configured()
                && !isLoopback(request.getRemoteAddr())) {
            throw trustedContextRequired();
        }
    }

    private String trustedRequestId(HttpServletRequest request, boolean required) {
        List<String> values = headerValues(request, TrustedHeaders.REQUEST_ID);
        if (values.isEmpty() && !required) {
            return RequestAttributes.requestId(request);
        }
        if (values.size() != 1 || !visibleAscii(values.getFirst())
                || values.getFirst().length() > properties.gateway().requestIdMaxLength()) {
            throw trustedContextRequired();
        }
        return values.getFirst();
    }

    private String routeId(HttpServletRequest request, String expected, boolean required) {
        List<String> values = headerValues(request, TrustedHeaders.ROUTE_ID);
        if (values.isEmpty() && !required) {
            return null;
        }
        if (values.size() != 1
                || !ROUTE_PATTERN.matcher(values.getFirst()).matches()
                || !allowedRouteIds.contains(values.getFirst())
                || !expected.equals(values.getFirst())) {
            throw trustedContextRequired();
        }
        request.setAttribute(RequestAttributes.ROUTE_ID, values.getFirst());
        return values.getFirst();
    }

    private String requiredSingle(HttpServletRequest request, String name) {
        List<String> values = headerValues(request, name);
        if (values.size() != 1) {
            throw trustedContextRequired();
        }
        String value = values.getFirst();
        if (value.isBlank()
                || value.length() > properties.gateway().trustedHeaderMaxLength()
                || !visibleAsciiWithSpaces(value)) {
            throw trustedContextRequired();
        }
        return value;
    }

    private void validateOptionalHeaders(HttpServletRequest request) {
        validateOptional(request, TrustedHeaders.TENANT_ID, 128, false);
        validateOptional(request, TrustedHeaders.ROLES, properties.gateway().trustedHeaderMaxLength(), true);
        validateOptional(request, TrustedHeaders.SOURCE_IP, 64, false);
        String sourceIp = optionalSingle(request, TrustedHeaders.SOURCE_IP);
        if (sourceIp != null && !isIpLiteral(sourceIp)) {
            throw trustedContextRequired();
        }
    }

    private void validateOptional(
            HttpServletRequest request,
            String name,
            int maximumLength,
            boolean encodedList) {
        String value = optionalSingle(request, name);
        if (value == null) {
            return;
        }
        if (value.length() > maximumLength || !visibleAsciiWithSpaces(value)) {
            throw trustedContextRequired();
        }
        if (encodedList) {
            try {
                EscapedListCodec.decode(value);
            } catch (IllegalArgumentException exception) {
                throw trustedContextRequired();
            }
        }
    }

    private String optionalSingle(HttpServletRequest request, String name) {
        List<String> values = headerValues(request, name);
        if (values.size() > 1) {
            throw trustedContextRequired();
        }
        return values.isEmpty() ? null : values.getFirst();
    }

    private static List<String> headerValues(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaders(name));
    }

    private static boolean visibleAscii(String value) {
        return !value.isEmpty() && value.chars().allMatch(character -> character >= 0x21 && character <= 0x7E);
    }

    private static boolean visibleAsciiWithSpaces(String value) {
        return !value.isBlank() && value.chars().allMatch(character -> character >= 0x20 && character <= 0x7E);
    }

    private static boolean isIpLiteral(String value) {
        if (!value.contains(":") && !value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private static boolean isLoopback(String value) {
        try {
            return InetAddress.getByName(value).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
